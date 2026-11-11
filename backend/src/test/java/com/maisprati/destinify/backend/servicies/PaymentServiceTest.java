package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.PaymentResponse;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import com.maisprati.destinify.backend.exceptions.BookingConflictException;
import com.maisprati.destinify.backend.exceptions.BookingNotFoundException;
import com.maisprati.destinify.backend.exceptions.ForbiddenException;
import com.maisprati.destinify.backend.exceptions.ResourceNotFoundException;
import com.maisprati.destinify.backend.repositories.BookingRepository;
import com.maisprati.destinify.backend.repositories.PaymentRepository;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.utils.mappers.PaymentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Test do PaymentService (pagamento simulado das reservas).

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @InjectMocks
    private PaymentService paymentService;

    private User owner;
    private Booking booking;
    private PaymentResponse response;

    @BeforeEach
    void setUp() {
        owner = TestDataFactory.client(1L);
        Hotel hotel = TestDataFactory.hotel(10L);
        Room room = TestDataFactory.room(100L, hotel, RoomType.SINGLE, 100f);
        booking = TestDataFactory.booking(7L, owner, hotel, List.of(room),
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(13), BookingStatus.PENDING);
        booking.setTotalPrice(new BigDecimal("350.50"));
        response = new PaymentResponse(3L, 7L, new BigDecimal("350.50"), PaymentStatus.PENDING, LocalDateTime.now(), null);
    }

    private Payment pendingPayment() {
        return new Payment(3L, booking, new BigDecimal("350.50"), PaymentStatus.PENDING, LocalDateTime.now(), null);
    }

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("cria um pagamento PENDING com o valor da reserva em 2 casas decimais")
        void semPagamentoPendente_criarNovo() {
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(7L, PaymentStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentMapper.paymentResponseMapper(any(Payment.class))).thenReturn(response);

            PaymentResponse result = paymentService.createPayment(7L, owner);

            assertThat(result).isEqualTo(response);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            Payment saved = captor.getValue();
            assertThat(saved.getBooking()).isSameAs(booking);
            assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
            assertThat(saved.getAmount()).isEqualByComparingTo("350.50");
            assertThat(saved.getAmount().scale()).isEqualTo(2); // exatamente 2 casas decimais
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getProcessedAt()).isNull();        // ainda não foi processado
        }

        @Test
        @DisplayName("arredonda para 2 casas com HALF_UP (100.125 vira 100.13, e não 100.12)")
        void valorComTresCasas_arredondaParaCima() {
            // 100.125f é exatamente representável em float, então o teste é determinístico.
            // Com RoundingMode.DOWN daria 100.12; com HALF_UP (regra "comercial") dá 100.13.
            booking.setTotalPrice(new BigDecimal("120.125"));
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(7L, PaymentStatus.PENDING))
                    .thenReturn(Optional.empty());
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(paymentMapper.paymentResponseMapper(any(Payment.class))).thenReturn(response);

            paymentService.createPayment(7L, owner);

            ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
            verify(paymentRepository).save(captor.capture());
            assertThat(captor.getValue().getAmount()).isEqualByComparingTo("100.13");
        }

        @Test
        @DisplayName("é idempotente: se já existe pagamento pendente, devolve ele e não cria outro")
        void comPagamentoPendente_reaproveita() {
            Payment existente = pendingPayment();
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(7L, PaymentStatus.PENDING))
                    .thenReturn(Optional.of(existente));
            when(paymentMapper.paymentResponseMapper(existente)).thenReturn(response);

            PaymentResponse result = paymentService.createPayment(7L, owner);

            assertThat(result).isEqualTo(response);
            verify(paymentRepository, never()).save(any(Payment.class)); // nada de pagamento duplicado
        }

        @Test
        @DisplayName("lança BookingNotFoundException quando a reserva não existe")
        void reservaInexistente_lancaNotFound() {
            when(bookingRepository.findById(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.createPayment(7L, owner))
                    .isInstanceOf(BookingNotFoundException.class)
                    .hasMessage("Booking with id 7 not found");
        }

        @Test
        @DisplayName("lança ForbiddenException quando a reserva é de outro usuário")
        void outroUsuario_lancaForbidden() {
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> paymentService.createPayment(7L, TestDataFactory.client(2L)))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("This reservation does not belong to this user");

            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("só reservas PENDING podem receber pagamento")
        void reservaNaoPendente_lancaConflito() {
            booking.setStatus(BookingStatus.CONFIRMED);
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> paymentService.createPayment(7L, owner))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("Only pending reservations can receive payment");
        }
    }

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("APPROVED: aprova o pagamento, define processedAt e CONFIRMA a reserva")
        void approved_confirmaReserva() {
            Payment payment = pendingPayment();
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findByIdAndBookingId(3L, 7L)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(payment)).thenReturn(payment);
            when(paymentMapper.paymentResponseMapper(payment)).thenReturn(response);

            PaymentResponse result = paymentService.processPayment(7L, 3L, owner, PaymentStatus.APPROVED);

            assertThat(result).isEqualTo(response);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(payment.getProcessedAt()).isNotNull();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            verify(bookingRepository).save(booking);
        }

        @Test
        @DisplayName("REJECTED: rejeita o pagamento e mantém a reserva PENDING")
        void rejected_naoConfirmaReserva() {
            Payment payment = pendingPayment();
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findByIdAndBookingId(3L, 7L)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(payment)).thenReturn(payment);
            when(paymentMapper.paymentResponseMapper(payment)).thenReturn(response);

            paymentService.processPayment(7L, 3L, owner, PaymentStatus.REJECTED);

            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.REJECTED);
            assertThat(payment.getProcessedAt()).isNotNull();
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
            verify(bookingRepository, never()).save(any(Booking.class));
        }

        @Test
        @DisplayName("recusa um resultado inválido (PENDING não é um resultado)")
        void resultadoInvalido_lancaConflito() {
            Payment payment = pendingPayment();
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findByIdAndBookingId(3L, 7L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.processPayment(7L, 3L, owner, PaymentStatus.PENDING))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("Invalid result");

            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING); // não mudou
        }

        @Test
        @DisplayName("recusa processar um pagamento que já foi processado")
        void pagamentoJaProcessado_lancaConflito() {
            Payment payment = pendingPayment();
            payment.setPaymentStatus(PaymentStatus.APPROVED);
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findByIdAndBookingId(3L, 7L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.processPayment(7L, 3L, owner, PaymentStatus.APPROVED))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("Payment is not pending");
        }

        @Test
        @DisplayName("recusa processar pagamento de reserva que não está PENDING")
        void reservaNaoPendente_lancaConflito() {
            booking.setStatus(BookingStatus.CANCELLED);
            Payment payment = pendingPayment();
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findByIdAndBookingId(3L, 7L)).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentService.processPayment(7L, 3L, owner, PaymentStatus.APPROVED))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("Payment is not pending");
        }

        @Test
        @DisplayName("lança ResourceNotFoundException quando o pagamento não pertence à reserva")
        void pagamentoInexistente_lancaNotFound() {
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findByIdAndBookingId(3L, 7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processPayment(7L, 3L, owner, PaymentStatus.APPROVED))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Payment not found");
        }

        @Test
        @DisplayName("lança ForbiddenException quando a reserva é de outro usuário")
        void outroUsuario_lancaForbidden() {
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> paymentService.processPayment(7L, 3L, TestDataFactory.client(2L), PaymentStatus.APPROVED))
                    .isInstanceOf(ForbiddenException.class);

            verify(paymentRepository, never()).save(any(Payment.class));
        }

        @Test
        @DisplayName("lança BookingNotFoundException quando a reserva não existe")
        void reservaInexistente_lancaNotFound() {
            when(bookingRepository.findById(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.processPayment(7L, 3L, owner, PaymentStatus.APPROVED))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("pay (atalho: aprova o pagamento pendente da reserva)")
    class Pay {

        @Test
        @DisplayName("localiza o pagamento pendente, aprova e confirma a reserva")
        void fluxoCompleto_aprovaEConfirma() {
            Payment payment = pendingPayment();
            // findById é chamado DUAS vezes (em pay e depois em processPayment); o mesmo stub atende as duas
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(7L, PaymentStatus.PENDING))
                    .thenReturn(Optional.of(payment));
            when(paymentRepository.findByIdAndBookingId(3L, 7L)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(payment)).thenReturn(payment);
            when(paymentMapper.paymentResponseMapper(payment)).thenReturn(response);

            PaymentResponse result = paymentService.pay(7L, owner);

            assertThat(result).isEqualTo(response);
            assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("lança BookingConflictException quando a reserva não está PENDING")
        void reservaNaoPendente_lancaConflito() {
            booking.setStatus(BookingStatus.CONFIRMED);
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

            assertThatThrownBy(() -> paymentService.pay(7L, owner))
                    .isInstanceOf(BookingConflictException.class)
                    .hasMessage("This booking is not pending");
        }

        @Test
        @DisplayName("lança ResourceNotFoundException quando não há pagamento pendente")
        void semPagamentoPendente_lancaNotFound() {
            when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
            when(paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(7L, PaymentStatus.PENDING))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.pay(7L, owner))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Payment pending not found");
        }

        @Test
        @DisplayName("lança BookingNotFoundException quando a reserva não existe")
        void reservaInexistente_lancaNotFound() {
            when(bookingRepository.findById(7L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.pay(7L, owner))
                    .isInstanceOf(BookingNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updatePendingAmount")
    class UpdatePendingAmount {

        @Test
        @DisplayName("atualiza o valor do pagamento pendente com o novo total da reserva")
        void atualizaValor() {
            Payment payment = pendingPayment();
            payment.setAmount(new BigDecimal("1.00")); // valor antigo
            booking.setTotalPrice(new BigDecimal("500.00"));
            when(paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(7L, PaymentStatus.PENDING))
                    .thenReturn(Optional.of(payment));

            paymentService.updatePendingAmount(booking);

            assertThat(payment.getAmount()).isEqualByComparingTo("500.00");
            assertThat(payment.getAmount().scale()).isEqualTo(2);
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("lança ResourceNotFoundException quando não há pagamento pendente")
        void semPagamentoPendente_lancaNotFound() {
            when(paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(7L, PaymentStatus.PENDING))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.updatePendingAmount(booking))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Pending payment not found");
        }
    }
}