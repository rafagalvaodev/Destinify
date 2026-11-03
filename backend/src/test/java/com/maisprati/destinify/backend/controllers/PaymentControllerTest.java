package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.PaymentResponse;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.exceptions.BookingConflictException;
import com.maisprati.destinify.backend.exceptions.BookingNotFoundException;
import com.maisprati.destinify.backend.exceptions.ForbiddenException;
import com.maisprati.destinify.backend.exceptions.ResourceNotFoundException;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.TokenService;
import com.maisprati.destinify.backend.servicies.PaymentService;
import com.maisprati.destinify.backend.support.TestAuth;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.support.WebTestConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("PaymentController")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private TokenService tokenService;      // exigido pelo AccessTokenFilter

    @MockitoBean
    private UserRepository userRepository;  // exigido pelo accessTokenFilter

    private final User cliente = TestDataFactory.client(1L);

    @Test
    @DisplayName("pagamento aprovado devolve 200 com os dados do pagamento")
    void pay_sucesso_retorna200() throws Exception {
        PaymentResponse response = new PaymentResponse(3L, 7L, new BigDecimal("350.50"),
                PaymentStatus.APPROVED, LocalDateTime.now(), LocalDateTime.now());
        when(paymentService.pay(eq(7L), any(User.class))).thenReturn(response);

        mockMvc.perform(post("/api/bookings/7/pay").with(TestAuth.as(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.bookingId").value(7))
                .andExpect(jsonPath("$.amount").value(350.50))
                .andExpect(jsonPath("$.paymentStatus").value("APPROVED"));
    }

    @Test
    @DisplayName("sem login: 403 e o service não é chamado")
    void pay_semLogin_retorna403() throws Exception {
        mockMvc.perform(post("/api/bookings/7/pay"))
                .andExpect(status().isForbidden());

        verify(paymentService, never()).pay(anyLong(), any(User.class));
    }

    @Test
    @DisplayName("reserva inexistente: 404")
    void pay_reservaInexistente_retorna404() throws Exception {
        when(paymentService.pay(eq(7L), any(User.class))).thenThrow(new BookingNotFoundException(7L));

        mockMvc.perform(post("/api/bookings/7/pay").with(TestAuth.as(cliente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Booking with id 7 not found"));
    }

    @Test
    @DisplayName("sem pagamento pendente: 404")
    void pay_semPagamentoPendente_retorna404() throws Exception {
        when(paymentService.pay(eq(7L), any(User.class)))
                .thenThrow(new ResourceNotFoundException("Payment pending not found"));

        mockMvc.perform(post("/api/bookings/7/pay").with(TestAuth.as(cliente)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reserva de outro usuário: 403 com ApiError")
    void pay_reservaDeOutro_retorna403() throws Exception {
        when(paymentService.pay(eq(7L), any(User.class)))
                .thenThrow(new ForbiddenException("This reservation does not belong to this user"));
        mockMvc.perform(post("/api/bookings/7/pay").with(TestAuth.as(cliente)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This reservation does not belong to this user"));
    }

    @Test
    @DisplayName("reserva que não está pendente: 409")
    void pay_reservaNaoPendente_retorna409() throws Exception {
        when(paymentService.pay(eq(7L), any(User.class)))
                .thenThrow(new BookingConflictException("This booking is not pending"));

        mockMvc.perform(post("/api/bookings/7/pay").with(TestAuth.as(cliente)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This booking is not pending"));
    }
}