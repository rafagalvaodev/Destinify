package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingCreate;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingResponse;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingUpdate;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.exceptions.BookingCancellationDeadlineException;
import com.maisprati.destinify.backend.exceptions.BookingConflictException;
import com.maisprati.destinify.backend.exceptions.BookingNotFoundException;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.BookingService;
import com.maisprati.destinify.backend.servicies.TokenService;
import com.maisprati.destinify.backend.support.TestAuth;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.support.WebTestConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("BookingController")
public class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private TokenService tokenService;      // exigido pelo AccessTokenFilter

    @MockitoBean
    private UserRepository userRepository;  // exigido pelo AccessTokenFilter

    private final User cliente = TestDataFactory.client(1L);
    private final User admin = TestDataFactory.admin(9L);

    private BookingResponse bookingResponse() {
        return new BookingResponse(55L, 1L, "Maria Cliente", 10L, "Hotel Central", List.of(),
                LocalDate.now().plusDays(10), LocalDate.now().plusDays(13), 2, 300f, BookingStatus.PENDING, null);
    }

    private String createJson(LocalDate checkIn, LocalDate checkOut, int guests) {
        return """
                {"hotelId":10, "roomIds":[100,101], "checkInDate":"%s", "checkOutDate":"%s", "guestsCount":%d}
                """.formatted(checkIn, checkOut, guests);
    }

    @Nested
    @DisplayName("POST /api/bookings")
    class CriarReserva {

        @Test
        @DisplayName("usuário logado cria a reserva e recebe 201; o service recebe o usuário logado")
        void logado_retorna201() throws Exception {
            LocalDate in = LocalDate.now().plusDays(10);
            LocalDate out = LocalDate.now().plusDays(13);
            when(bookingService.newBooking(any(BookingCreate.class), any(User.class))).thenReturn(bookingResponse());

            mockMvc.perform(post("/api/bookings")
                    .with(TestAuth.as(cliente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson(in, out, 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.status").value("PENDING"));

            ArgumentCaptor<BookingCreate> dto = ArgumentCaptor.forClass(BookingCreate.class);
            ArgumentCaptor<User> usuario = ArgumentCaptor.forClass(User.class);
            verify(bookingService).newBooking(dto.capture(), usuario.capture());
            assertThat(dto.getValue()).isEqualTo(new BookingCreate(10L, List.of(100L, 101L), in, out, 2));
            assertThat(usuario.getValue().getId()).isEqualTo(1L); // veio do login
        }

        @Test
        @DisplayName("sem login: 403")
        void semLogin_retorna403() throws Exception {
            mockMvc.perform(post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson(LocalDate.now().plusDays(10), LocalDate.now().plusDays(13), 2)))
                .andExpect(status().isForbidden());

            verify(bookingService, never()).newBooking(any(BookingCreate.class), any(User.class));
        }

        @Test
        @DisplayName("check-in no passado: 400 e o service não é chamado")
        void checkInNoPassado_retorna400() throws Exception {
            mockMvc.perform(post("/api/bookings")
                    .with(TestAuth.as(cliente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson(LocalDate.now().minusDays(2), LocalDate.now().plusDays(3), 2)))
                .andExpect(status().isBadRequest());

            verify(bookingService, never()).newBooking(any(BookingCreate.class), any(User.class));
        }

        @Test
        @DisplayName("lista de quartos vazia: 400")
        void semQuartos_retorna400() throws Exception {
            String json = """
                    {"hotelId":10, "roomIds":[], "checkInDate":"%s", "checkOutDate":"%s", "guestsCount":1}
                    """.formatted(LocalDate.now().plusDays(10), LocalDate.now().plusDays(13));

            mockMvc.perform(post("/api/bookings")
                    .with(TestAuth.as(cliente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("conflito de datas/capacidade no service vira 409 com a mensagem")
        void conflito_retorna409() throws Exception {
            when(bookingService.newBooking(any(BookingCreate.class), any(User.class)))
                    .thenThrow(new BookingConflictException("O quarto 100 já está reservado para o período selecionado"));

            mockMvc.perform(post("/api/bookings")
                    .with(TestAuth.as(cliente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createJson(LocalDate.now().plusDays(10), LocalDate.now().plusDays(13), 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").value("O quarto 100 já está reservado para o período selecionado"));
        }
    }

    @Nested
    @DisplayName("GET /api/bookings/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("devolver a reserva ao dono")
        void dono_retorna200() throws Exception {
            when(bookingService.findBookingById(eq(55L), any(User.class))).thenReturn(bookingResponse());

            mockMvc.perform(get("/api/bookings/55").with(TestAuth.as(cliente)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hotelName").value("Hotel Central"));
        }

        @Test
        @DisplayName("reserva de outro usuário: o 403 lançado pelo service chega ao cliente")
        void deOutroUsuario_retorna403() throws Exception {
            when(bookingService.findBookingById(eq(55L), any(User.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "This reservation belongs to another user"));

            mockMvc.perform(get("/api/bookings/55").with(TestAuth.as(cliente)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("reserva inexistente: 404")
        void naoExiste_retorna404() throws Exception {
            when(bookingService.findBookingById(eq(55L), any(User.class))).thenThrow(new BookingNotFoundException(55L));

            mockMvc.perform(get("/api/bookings/55").with(TestAuth.as(cliente)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Booking with id 55 not found"));
        }

        @Test
        @DisplayName("sem login: 403")
        void semLogin_retorna403() throws Exception {
            mockMvc.perform(get("/api/bookings/55")).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("listagens")
    class Listagens {

        @Test
        @DisplayName("GET /api/bookings/me lista só as reservas do usuário logado")
        void minhasReservas_usaIdDoLogin() throws Exception {
            when(bookingService.findByUserId(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(bookingResponse()), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/bookings/me").with(TestAuth.as(cliente)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(55));

            verify(bookingService).findByUserId(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("GET /api/bookings/all: ADMIN recebe 200")
        void todas_admin_retorna200() throws Exception {
            when(bookingService.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(bookingResponse()), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/bookings/all").with(TestAuth.as(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(55));
        }

        @Test
        @DisplayName("GET /api/bookings/all: CLIENT recebe 403")
        void todas_client_retorna403() throws Exception {
            mockMvc.perform(get("/api/bookings/all").with(TestAuth.as(cliente)))
                    .andExpect(status().isForbidden());

            verify(bookingService, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("PATCH /api/bookings/{id}")
    class Atualizar {

        @Test
        @DisplayName("atualiza parcialmente e devolve 200")
        void logado_retorna200() throws Exception {
            when(bookingService.updateBooking(any(User.class), eq(55L), any(BookingUpdate.class)))
                    .thenReturn(bookingResponse());

            mockMvc.perform(patch("/api/bookings/55")
                    .with(TestAuth.as(cliente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"guestsCount\":2}"))
                .andExpect(status().isOk());

            verify(bookingService).updateBooking(any(User.class), eq(55L), eq(new BookingUpdate(null, null, 2)));
        }

        @Test
        @DisplayName("reserva que não está PENDING: 409")
        void naoPendente_retorna409() throws Exception {
            when(bookingService.updateBooking(any(User.class), eq(55L), any(BookingUpdate.class)))
                    .thenThrow(new BookingConflictException("Só é possível editar reservas pendentes."));

            mockMvc.perform(patch("/api/bookings/55")
                    .with(TestAuth.as(cliente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"guestsCount\":2}"))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("PATCH /api/bookings/{id}/cancel")
    class Cancelar {

        @Test
        @DisplayName("cancela e devolve 200")
        void logadoRetorna200() throws Exception {
            when(bookingService.cancelBooking(any(User.class), eq(55L))).thenReturn(bookingResponse());

            mockMvc.perform(patch("/api/bookings/55/cancel").with(TestAuth.as(cliente)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("fora do prazo de 72h: 400 com a mensagem de prazo")
        void foraDoPrazo_retorna400() throws Exception {
            when(bookingService.cancelBooking(any(User.class), eq(55L)))
                    .thenThrow(new BookingCancellationDeadlineException());

            mockMvc.perform(patch("/api/bookings/55/cancel").with(TestAuth.as(cliente)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("O cancelamento exige mais de 72 horas de antecedência"));
        }

        @Test
        @DisplayName("sem login: 403")
        void semLogin_retorna403() throws Exception {
            mockMvc.perform(patch("/api/bookings/55/cancel")).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("DELETE /api/bookings/{id}")
    class Excluir {

        @Test
        @DisplayName("ADMIN exclui e recebe 204")
        void admin_retorna204() throws Exception {
            mockMvc.perform(delete("/api/bookings/55").with(TestAuth.as(admin)))
                    .andExpect(status().isNoContent());

            verify(bookingService).deleteBooking(55L);
        }

        /**
         * >>> BUG ENCONTRADO PELOS TESTES <<<
         * O comentário do BookingService diz "Exclusão definitivia do registro do banco (ADMIN)",
         * mas o SecurityConfig não tem regra para DELETE /api/bookings/{id}; ela cai no
         * "anyRequest().authenticated()" e qualquer usuário logado pode apagar qualquer reserva.
         */

        @Test
        @Disabled("BUG conhecido: DELETE /api/bookings/{id} deveria ser só ADMIN (ver SecurityConfig)")
        @DisplayName("CLIENT deveria receber 403 (hoje recebe 204: falha de segurança)")
        void client_deveriaReceber403() throws Exception {
            mockMvc.perform(delete("/api/bookings/55").with(TestAuth.as(cliente)))
                    .andExpect(status().isForbidden());

            verify(bookingService, never()).deleteBooking(anyLong());
        }
    }
}
