package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingCreate;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingResponse;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingSummary;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingUpdate;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import com.maisprati.destinify.backend.exceptions.BookingCancellationDeadlineException;
import com.maisprati.destinify.backend.exceptions.BookingConflictException;
import com.maisprati.destinify.backend.exceptions.BookingNotFoundException;
import com.maisprati.destinify.backend.exceptions.RoomNotFoundException;
import com.maisprati.destinify.backend.repositories.BookingRepository;
import com.maisprati.destinify.backend.repositories.RoomRepository;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.utils.mappers.BookingMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService")
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PromotionService promotionService;

    @InjectMocks
    private BookingService bookingService;

    // Dados comuns a todos os testes (recriados a cada teste pelo @BeforeEach)
    private User user;
    private Hotel hotel;
    private Room single;    // id 100, capacidade 1, diária 100
    private Room suite;     // id 101, capacidade 2, diária 200
    private LocalDate checkIn;
    private LocalDate checkOut;
    private BookingResponse response;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.client(1L);
        hotel = TestDataFactory.hotel(10L);
        single = TestDataFactory.room(100L, hotel, RoomType.SINGLE, 100f);
        suite = TestDataFactory.room(101L, hotel, RoomType.SUITE, 200f);
        checkIn = LocalDate.now().plusDays(10);
        checkOut = LocalDate.now().plusDays(13); // 3 noites
        response = TestDataFactory.bookingResponse(55L, 1L, "Maria Cliente", 10L, "Hotel Central", List.of(),
                checkIn, checkOut, 1, new BigDecimal("300"), BookingStatus.PENDING, null);
    }

    private BookingCreate createDto(Long hotelId, List<Long> roomIds, int guests) {
        return TestDataFactory.bookingCreate(hotelId, roomIds, checkIn, checkOut, guests);
    }

    private BookingSummary summary(String base, String discount, String total) {
        return new BookingSummary(3, 1, List.of(),
                new BigDecimal(base), new BigDecimal(discount), new BigDecimal(total), List.of(),
                "Cancelamento permitido com mais de 72 horas de antecêdencia do check-in às 14h.",
                10L, "Hotel Central", checkIn, checkOut, List.of(100L));
    }

    private Booking pendingBookingOf(User owner) {
        Booking booking = TestDataFactory.booking(55L, owner, hotel, List.of(single), checkIn, checkOut, BookingStatus.PENDING);
        booking.setTotalPrice(new BigDecimal("300.00"));
        return booking;
    }

    // newBooking

    @Nested
    @DisplayName("newBooking")
    class NewBooking {

        @Test
        @DisplayName("com um quarto livre: calcula o preço, salva como PENDING e cria o pagamento")
        void umQuartoLivre_salvaECriaPagamento() {
            // Arrange
            when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(single));
            // o 4º argumento CANCELLED prova que reservas canceladas não bloqueiam datas.
            when(bookingRepository.findOverlappingBookings(100L, checkIn, checkOut, BookingStatus.CANCELLED))
                    .thenReturn(List.of());
            // thenAnswer: em vez de um valor fixo, calculamos a resposta. Aqui simulamos o banco:
            // devolve a mesma reserva, mas já com id (como o INSERT faria).
            when(promotionService.calculate(List.of(single), checkIn, checkOut, 1, null, null))
                    .thenReturn(summary("330.00", "30.00", "300.00"));
            when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
                Booking b = invocation.getArgument(0);
                b.setId(55L);
                return b;
            });
            when(bookingMapper.bookingResponseMapper(any(Booking.class))).thenReturn(response);

            // Act
            BookingResponse result = bookingService.newBooking(createDto(10L, List.of(100L), 1), user);

            // Assert 1: resposta
            assertThat(result).isEqualTo(response);

            // Assert 2: o que foi persistido
            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(captor.capture());
            Booking saved = captor.getValue();
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(saved.getHotel()).isSameAs(hotel);
            assertThat(saved.getRooms()).containsExactly(single);
            assertThat(saved.getCheckInDate()).isEqualTo(checkIn);
            assertThat(saved.getCheckOutDate()).isEqualTo(checkOut);
            assertThat(saved.getGuestsCount()).isEqualTo(1);
            
            assertThat(saved.getStatus()).isEqualTo(BookingStatus.PENDING);

            assertThat(saved.getBasePrice()).isEqualByComparingTo("330.00");
            assertThat(saved.getDiscountTotal()).isEqualByComparingTo("30.00");
            assertThat(saved.getTotalPrice()).isEqualByComparingTo("300.00");

            // Assert 3: ORDEM -> primeiro salva a reserva, depois cria o pagamento com o id gerado
            InOrder ordem = inOrder(bookingRepository, paymentService);
            ordem.verify(bookingRepository).save(any(Booking.class));
            ordem.verify(paymentService).createPayment(55L, user);
        }

        @Test
        @DisplayName("com vários quartos: soma as diárias, remove ids repetidos e trava em ordem crescente")
        void variosQuartos_somaDiariasEOrdenaIds() {
            // ids propositalmente fora de ordem e com repetição: [101, 100, 100]
            BookingCreate dto = createDto(10L, List.of(101L, 100L, 100L), 3);

            doReturn(Optional.of(single)).when(roomRepository).findByIdForUpdate(100L);
            doReturn(Optional.of(suite)).when(roomRepository).findByIdForUpdate(101L);
            doReturn(List.of()).when(bookingRepository).findOverlappingBookings(100L, checkIn,
                    checkOut, BookingStatus.CANCELLED);
            doReturn(List.of()).when(bookingRepository).findOverlappingBookings(101L, checkIn,
                    checkOut, BookingStatus.CANCELLED);
            when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
            when(bookingMapper.bookingResponseMapper(any(Booking.class))).thenReturn(response);

            bookingService.newBooking(dto, user);

            ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
            verify(bookingRepository).save(captor.capture());
            // (100 + 200) por noite x 3 noite = 900
            assertThat(captor.getValue().getTotalPrice()).isEqualTo(900f);
            assertThat(captor.getValue().getRooms()).containsExactly(single, suite);

            // Travar em ordem crescentes (100 depois 101) evita deadLock entre reserevas simultâneas
            InOrder ordem = inOrder(roomRepository);
            ordem.verify(roomRepository).findByIdForUpdate(100L);
            ordem.verify(roomRepository).findByIdForUpdate(101L);
            // o id 100 apareceu duas vezes no pedido, mas só foi processado uma vez
            verify(roomRepository, times(1)).findByIdForUpdate(100L);
        }

        @ParameterizedTest(name = "check-out = check-in + {0} dia(s)")
        @ValueSource(ints = {0, -1})
        @DisplayName("recusa quando o check-out não é posterior ao check-in")
        void checkOutNaoPosterior_lancaConflito(int diasDepoisDoCheckIn) {
            BookingCreate dto = TestDataFactory.bookingCreate(10L, List.of(100L), checkIn, checkIn.plusDays(diasDepoisDoCheckIn), 1);

            assertThatThrownBy(() -> bookingService.newBooking(dto, user))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("A data de check-out deve ser posterior à data de check-in");

            // A validação de datas acontece antes de tocar no banco
            verify(roomRepository, never()).findByIdForUpdate(anyLong());
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("recusa quando o quarto não existe")
        void quartoInexistente_lancaRoomNotFound() {
            when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.newBooking(createDto(10L, List.of(100L), 1), user))
                    .isInstanceOf(RoomNotFoundException.class)
                    .hasMessage("Room with id 100 not found");
        }

        @Test
        @DisplayName("recusa quando o quarto pertence a outro hotel")
        void quartoDoOutroHotel_lancaConflito() {
            when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(single)); // quarto é do hotel 10

            // pedido diz que o hotel é o 99
            assertThatThrownBy(() -> bookingService.newBooking(createDto(99L, List.of(100L), 1), user))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("Todos os quartos devem pertencer ao hotel informado");

            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("recusa quando o quarto já está reservado em alguma data do período")
        void periodoComSobreposicao_lancaConflito() {
            Booking existente = pendingBookingOf(TestDataFactory.client(2L));
            when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(single));
            when(bookingRepository.findOverlappingBookings(100L, checkIn, checkOut, BookingStatus.CANCELLED))
                    .thenReturn(List.of(existente));

            assertThatThrownBy(() -> bookingService.newBooking(createDto(10L, List.of(100L), 1), user))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("O quarto 100 já está reservado para o período selecionado");

            verify(bookingRepository, never()).save(any(Booking.class));
            verify(paymentService, never()).createPayment(anyLong(), any(User.class));
        }

        @Test
        @DisplayName("recusa quando há mais hóspedes do que a capacidade dos quartos")
        void hospedesAcimaDaCapacidade_lancaConflito() {
            when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(single));    // capacidade 1
            when(bookingRepository.findOverlappingBookings(100L, checkIn, checkOut, BookingStatus.CANCELLED))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> bookingService.newBooking(createDto(10L, List.of(100L), 2), user))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("Os quartos selecionados comportam no máximo 1 hóspede(s)");

            verify(bookingRepository, never()).save(any(Booking.class));
        }
    }

    // consultas (findBookingById, findAll,  findByUserId)

    @Nested
    @DisplayName("consultas")
    class Consultas {

        @Test
        @DisplayName("findBookingById devolve a reserva ao próprio dono")
        void findBookingById_dono_retornaReserva() {
            Booking booking = pendingBookingOf(user);
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));
            when(bookingMapper.bookingResponseMapper(booking)).thenReturn(response);

            assertThat(bookingService.findBookingById(55L, user)).isEqualTo(response);
        }

        @Test
        @DisplayName("findBookingById nega (403) quando a reserva é de outro usuário")
        void findBookingById_outroUsuario_lancaForbidden() {
            Booking booking = pendingBookingOf(TestDataFactory.client(2L)); // dono é o usuário 2
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.findBookingById(55L, user)) // quem pede é o usuário 1
                    .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                        assertThat(ex.getReason()).isEqualTo("This reservation belongs to another user");
                    });
            verify(bookingMapper, never()).bookingResponseMapper(any(Booking.class)); // nada vazia
        }

        @Test
        @DisplayName("findBookingById lança BookingNotFoundException quando não existe")
        void findBookingById_naoExiste_lancaNotFound() {
            when(bookingRepository.findById(55L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.findBookingById(55L, user))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessage("Booking with id 55 not found");
        }

        @Test
        @DisplayName("findAll converte a página de reservas")
        void findAll_convertePagina() {
            Pageable pageable = PageRequest.of(0, 10);
            Booking booking = pendingBookingOf(user);
            when(bookingRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
            when(bookingMapper.bookingResponseMapper(booking)).thenReturn(response);

            Page<BookingResponse> result = bookingService.findAll(pageable);

            assertThat(result.getContent()).containsExactly(response);
        }

        @Test
        @DisplayName("findByUserId consulta só as reservas do usuário informado")
        void findByUserId_convertePagina() {
            Pageable pageable = PageRequest.of(0, 10);
            Booking booking = pendingBookingOf(user);
            when(bookingRepository.findByUserId(1L, pageable)).thenReturn(new PageImpl<>(List.of(booking), pageable, 1));
            when(bookingMapper.bookingResponseMapper(booking)).thenReturn(response);

            Page<BookingResponse> result = bookingService.findByUserId(1L, pageable);

            assertThat(result.getContent()).containsExactly(response);
        }
    }

    // updateBooking

    @Nested
    @DisplayName("updateBooking")
    class UpdateBooking {

        @Test
        @DisplayName("troca as datas, recalcula o preço e atualiza o valor do pagamento pendente")
        void novasDatas_recalculaPrecoEAtualizaPagamento() {
            Booking booking = pendingBookingOf(user);
            LocalDate novoCheckIn = LocalDate.now().plusDays(20);
            LocalDate novoCheckOut = LocalDate.now().plusDays(25); // 5 noites
            BookingUpdate dto = TestDataFactory.bookingUpdate(novoCheckIn, novoCheckOut, null);

            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));
            when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(single));
            when(bookingRepository.findOverlappingBookingsExcludingBooking(
                    100L, novoCheckIn, novoCheckOut, BookingStatus.CANCELLED, 55L))
                    .thenReturn(List.of());
            when(bookingRepository.save(booking)).thenReturn(booking);
            when(bookingMapper.bookingResponseMapper(booking)).thenReturn(response);

            BookingResponse result = bookingService.updateBooking(user, 55L, dto);

            assertThat(result).isEqualTo(response);
            assertThat(booking.getCheckInDate()).isEqualTo(novoCheckIn);
            assertThat(booking.getCheckOutDate()).isEqualTo(novoCheckOut);
            assertThat(booking.getGuestsCount()).isEqualTo(1);  // não veio no DTO: mantém
            assertThat(booking.getTotalPrice()).isEqualTo(500f); // 100 x 5 noites
        verify(paymentService).updatePendingAmount(booking);            // pagamento acompanha o novo valor
        }

        @Test
        @DisplayName("atualização parcial: só guestsCount mantém as datas antigas")
        void soGuestsCount_mantemDatas() {
            Booking booking = pendingBookingOf(user);
            BookingUpdate dto = TestDataFactory.bookingUpdate(null, null, 1);

            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));
            when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(single));
            when(bookingRepository.findOverlappingBookingsExcludingBooking(
                    100L, checkIn, checkOut, BookingStatus.CANCELLED, 55L))
                    .thenReturn(List.of());
            when(bookingRepository.save(booking)).thenReturn(booking);
            when(bookingMapper.bookingResponseMapper(booking)).thenReturn(response);

            bookingService.updateBooking(user, 55L, dto);

            assertThat(booking.getCheckInDate()).isEqualTo(checkIn);
            assertThat(booking.getCheckOutDate()).isEqualTo(checkOut);
            assertThat(booking.getTotalPrice()).isEqualTo(300f);
        }

        @Test
        @DisplayName("nega (403) quando a reserva é de outro usuário")
        void outroUsuario_lancaForbidden() {
            Booking booking = pendingBookingOf(TestDataFactory.client(2L));
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.updateBooking(user, 55L, TestDataFactory.bookingUpdate(null, null, 1)))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex-> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("só permite editar reservas PENDING")
        void reservaConfirmada_lancaConflito() {
            Booking booking = pendingBookingOf(user);
            booking.setStatus(BookingStatus.CONFIRMED);
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.updateBooking(user, 55L, TestDataFactory.bookingUpdate(null, null, 1)))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("Só é possível editar reservas pendentes.");
        }

        @Test
        @DisplayName("recusa novas datas em que o check-out não é posterior ao check-in")
        void datasInvalidas_lancaConflito() {
            Booking booking = pendingBookingOf(user);
            // só o check-out vem no DTO e cai antes do check-in existente
            BookingUpdate dto = TestDataFactory.bookingUpdate(null, LocalDate.now().plusDays(5), null);
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.updateBooking(user, 55L, dto))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("A data de check-out deve ser posterior à data de check-in");

            verify(roomRepository, never()).findByIdForUpdate(anyLong());
        }

        @Test
        @DisplayName("recusa quando o quarto já está reservado no novo período")
        void novoPeriodoComSobreposicao_lancaConflito() {
            Booking booking = pendingBookingOf(user);
            BookingUpdate dto = TestDataFactory.bookingUpdate(null, null, 1);
            Booking outraReserva = pendingBookingOf(TestDataFactory.client(2L));

            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));
            when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(single));
            when(bookingRepository.findOverlappingBookingsExcludingBooking(
                    100L, checkIn, checkOut, BookingStatus.CANCELLED, 55L))
                    .thenReturn(List.of(outraReserva));

            assertThatThrownBy(() -> bookingService.updateBooking(user, 55L, dto))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("O quarto 100 já está reservado para o novo período selecionado");

            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("recusa quando o novo número de hóspedes excede a capacidade")
        void hospedesAcimaDaCapacidade_lancaConflito() {
            Booking booking = pendingBookingOf(user);   // quarto single: capacidade 1
            BookingUpdate dto = TestDataFactory.bookingUpdate(null, null, 5);

            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));
            when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(single));
            when(bookingRepository.findOverlappingBookingsExcludingBooking(
                    100L, checkIn, checkOut, BookingStatus.CANCELLED, 55L))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> bookingService.updateBooking(user, 55L, dto))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("O quarto selecionado comporta no máximo 1 hospede(s)");
        }

        @Test
        @DisplayName("lança BookingNotFoundException quando a reserva não existe")
        void naoExiste_lancaNotFound() {
            when(bookingRepository.findById(55L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.updateBooking(user, 55L, TestDataFactory.bookingUpdate(null, null, 1)))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }

    // cancelBooking

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("cancela quando faltam bem mais de 72h para o check-in")
        void comAntecedencia_cancela() {
            Booking booking = pendingBookingOf(user); // check-in daqui a 10 dias
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));
            when(bookingRepository.save(booking)).thenReturn(booking);
            when(bookingMapper.bookingResponseMapper(booking)).thenReturn(response);

            BookingResponse result = bookingService.cancelBooking(user, 55L);

            assertThat(result).isEqualTo(response);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("recusa o cancelamento com menos de 72h de antecedência")
        void foraDoPrazo_lancaDeadlineException() {
            Booking booking = TestDataFactory.booking(55L, user, hotel, List.of(single),
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), BookingStatus.CONFIRMED);
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(user, 55L))
                    .isInstanceOf(BookingCancellationDeadlineException.class)
                    .hasMessage("O cancelamento exige mais de 72 horas de antecedência");

            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("nega (403) quando a reserva é de outro usuário")
        void outroUsuario_lancaForbidden() {
            Booking booking = pendingBookingOf(TestDataFactory.client(2L));
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(user, 55L))
                    .isInstanceOfSatisfying(ResponseStatusException.class,
                            ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("recusa cancelar uma reseva que já está cancelada")
        void jaCancelada_lancaConflito() {
            Booking booking = pendingBookingOf(user);
            booking.setStatus(BookingStatus.CANCELLED);
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(user, 55L))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("Esta reserva já esta cancelada");
        }

        @Test
        @DisplayName("recusa cancelar uma reserva concluída")
        void concluida_lancaConflito() {
            Booking booking = pendingBookingOf(user);
            booking.setStatus(BookingStatus.COMPLETED);
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> bookingService.cancelBooking(user, 55L))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("Não é possível cancelar uma reserva já concluída");
        }

        @Test
        @DisplayName("lança BookingNotFoundException quando a reserva não existe")
        void naoExiste_lancaNotFound() {
            when(bookingRepository.findById(55L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.cancelBooking(user, 55L))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }

    // deleteBooking (exclusão definitiva, uso administrativo)

    @Nested
    @DisplayName("deleteBooking")
    class DeleteBooking {

        @Test
        @DisplayName("remove a reserva existente")
        void existe_remove() {
            Booking booking = pendingBookingOf(user);
            when(bookingRepository.findById(55L)).thenReturn(Optional.of(booking));

            bookingService.deleteBooking(55L);

            verify(bookingRepository).delete(booking);
        }

        @Test
        @DisplayName("lança BookingNotFoundException quando não existe")
        void naoExiste_lancaNotFound() {
            when(bookingRepository.findById(55L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.deleteBooking(55L))
                    .isInstanceOf(BookingNotFoundException.class);

            verify(bookingRepository, never()).delete(any(Booking.class));
        }
    }
}
