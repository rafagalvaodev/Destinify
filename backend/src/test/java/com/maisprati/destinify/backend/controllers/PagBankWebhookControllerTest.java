package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.integrations.pagbank.PagBankWebhookSignature;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.TokenService;
import com.maisprati.destinify.backend.support.WebTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PagBankWebhookController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("PagBankWebhookController")
class PagBankWebhookControllerTest {

    private static final String CORPO = "{\"id\":\"ORDE_1\",\"status\":\"PAID\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagBankWebhookSignature signature;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("a rota é pública: o POST sem login não recebe 403")
    void webhook_semLogin_retorna200() throws Exception{
        mockMvc.perform(post("/api/payments/pagbank/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORPO))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("repassa o cropo (em bytes) e o cabeçalho x-authenticity-token para a validação")
    void webhook_repassaCorpoECabecalhoParaAValidacao() throws Exception {
        mockMvc.perform(post("/api/payments/pagbank/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-authenticity-token", "abc123")
                .content(CORPO))
            .andExpect(status().isOk());

        verify(signature).valida(aryEq(CORPO.getBytes(StandardCharsets.UTF_8)), eq("acb123"));
    }

    @Test
    @DisplayName("só aceita POST: GET na rota não é aceita")
    void webhook_get_naoEhAceito() throws Exception {
        mockMvc.perform(get("/api/payments/pagbank/webhook"))
                .andExpect(status().is4xxClientError());
    }
}
