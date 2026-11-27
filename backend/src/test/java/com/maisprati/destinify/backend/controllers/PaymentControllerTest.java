package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
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

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("PaymentController - segurança das rotas")
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
    @DisplayName("rota antiga/pay não deve mais existetir")
void rotaAntigaPay_naoExiste() throws Exception {

    mockMvc.perform(post("/api/bookings/7/pay")
            .with(TestAuth.as(cliente)))
            .andExpect(status().isNotFound());

    verifyNoInteractions(paymentService);
    }
}
