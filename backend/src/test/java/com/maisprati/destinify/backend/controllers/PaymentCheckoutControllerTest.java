package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.CheckoutResponse;
import com.maisprati.destinify.backend.exceptions.BookingConflictException;
import com.maisprati.destinify.backend.exceptions.PaymentGatewayException;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.PaymentService;
import com.maisprati.destinify.backend.servicies.TokenService;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("PaymentController - checkout")
class PaymentCheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private TokenService tokenService;          // exigido pelo AccessTokenFilter

    @MockitoBean
    private UserRepository userRepository;      // exigido pelo AccessTokenFilter

    private final User cliente = TestDataFactory.client(1L);

    @Test
    @DisplayName("usuário logado recebe 200 com o link da página de pagamento")
    void checkout_sucesso_retorna200ComUrl() throws Exception {
        CheckoutResponse response = new CheckoutResponse(3L, 7L,
                "https://pagamento.pagbank.com.br/pagamento?code=XYZ", LocalDateTime.of(2030, 1, 10, 12, 30));
        when(paymentService.startCheckout(eq(7L), any(User.class))).thenReturn(response);

        mockMvc.perform(post("/api/bookings/7/checkout").with(TestAuth.as(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(3))
                .andExpect(jsonPath("$.bookingId").value(7))
                .andExpect(jsonPath("$.checkoutUrl").value("https://pagamento.pagbank.com.br/pagamento?code=XYZ"));
    }

    @Test
    @DisplayName("sem login: 403 e o service não é chamado")
    void checkout_reservaNaoPendente_retorna409() throws Exception {
        when(paymentService.startCheckout(eq(7L), any(User.class)))
                .thenThrow(new BookingConflictException("Only pending reservations can receive payment"));

        mockMvc.perform(post("/api/bookings/7/checkout").with(TestAuth.as(cliente)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only pending reservations can receive payment"));
    }

    @Test
    @DisplayName("falha no PagBank: 502 com mensage clara")
    void checkout_falhaNoPagBank_retorna502() throws Exception {
        when(paymentService.startCheckout(eq(7L), any(User.class)))
                .thenThrow(new PaymentGatewayException("Não foi possível criar o checkout no PagBank",
                        new RuntimeException("causa")));

        mockMvc.perform(post("/api/bookings/7/checkout").with(TestAuth.as(cliente)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Não foi possível criar o checkout no PagBank"));
    }
}
