package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.integrations.pagbank.PagBankWebhookSignature;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.PagBankWebhookService;
import com.maisprati.destinify.backend.servicies.PagBankWebhookService.Resultado;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PagBankWebhookController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("PagBankWebhookController")
class PagBankWebhookControllerTest {

    private static final String CORPO = "{\"id\":\"ORDE_1\",\"status\":\"PAID\"}";
    private static final byte[] CORPO_BYTES = CORPO.getBytes(StandardCharsets.UTF_8);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PagBankWebhookSignature signature;

    @MockitoBean
    private PagBankWebhookService webhookService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("assinatura inválida: 401 e nada é processado")
    void assinaturaInvalida_retorna401() throws Exception {
        // o mock de signature devolve false por padrão (assinatura inválida)
        mockMvc.perform(post("/api/payments/pagbank/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-authenticity-token", "falsa")
                        .content(CORPO))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(webhookService);
    }

    @Test
    @DisplayName("sem o cabeçalho de assinatura: 401")
    void semCabecalho_retorna401() throws Exception {
        mockMvc.perform(post("/api/payments/pagbank/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CORPO))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(webhookService);
    }

    @Test
    @DisplayName("assinatura válida: processa o corpo (em bytes) e responde 200, sem precisar de login")
    void assinaturaValida_processaERetorna200() throws Exception {
        when(signature.valida(aryEq(CORPO_BYTES), eq("assinatura-boa"))).thenReturn(true);
        when(webhookService.processar(aryEq(CORPO_BYTES))).thenReturn(Resultado.APROVADO);

        mockMvc.perform(post("/api/payments/pagbank/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-authenticity-token", "assinatura-boa")
                .content(CORPO))
            .andExpect(status().isOk());

        verify(webhookService).processar(aryEq(CORPO_BYTES));
    }

    @Test
    @DisplayName("resultado 'ignorado' também responde 200 (o PagBank não precisa reenviar)")
    void resultadoIgnorado_retorna200() throws Exception {
        when(signature.valida(aryEq(CORPO_BYTES), eq("assinatura-boa"))).thenReturn(true);
        when(webhookService.processar(aryEq(CORPO_BYTES))).thenReturn(Resultado.IGNORADO);

        mockMvc.perform(post("/api/payments/pagbank/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-authenticity-token", "assinatura-boa")
                .content(CORPO))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("falha ao processar (ex.: PagBank fora do ar): 500, para o PagBank reenviar")
    void falhaAoProcessar_retorna500() throws Exception {
        when(signature.valida(aryEq(CORPO_BYTES), eq("assinatura-boa"))).thenReturn(true);
        when(webhookService.processar(aryEq(CORPO_BYTES))).thenThrow(new IllegalStateException("PagBank fora do ar"));

        mockMvc.perform(post("/api/payments/pagbank/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("x-authenticity-token", "assinatura-boa")
                        .content(CORPO))
                .andExpect(status().isInternalServerError());

    }

    @Test
    @DisplayName("só aceita POST")
    void webhook_get_naoEhAceito() throws Exception {
        mockMvc.perform(get("/api/payments/pagbank/webhook"))
                .andExpect(status().is4xxClientError());
    }
}
