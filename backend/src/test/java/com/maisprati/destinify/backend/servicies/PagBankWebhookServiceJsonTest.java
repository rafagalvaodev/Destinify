package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.integrations.pagbank.PagBankClient;
import com.maisprati.destinify.backend.servicies.PagBankWebhookService.Resultado;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagBankWebhookService - leitura do JSON")
class PagBankWebhookServiceJsonTest {

    @Mock
    private PagBankClient pagBankClient;

    @Mock
    private PaymentConfirmationService confirmationService;

    @InjectMocks
    private PagBankWebhookService service;

    @Test
    @DisplayName("lê um JSON de checkout expirado (com acento no corpo) e limpa o checkout")
    void jsonValido_eProcessado() {
        when(confirmationService.clearExpiredCheckout("CHEC_1")).thenReturn(true);
        byte[] corpo = "{\"id\":\"CHEC_1\",\"status\":\"EXPIRED\",\"cidade\":\"São Paulo\"}"
                .getBytes(StandardCharsets.UTF_8);

        assertThat(service.processar(corpo)).isEqualTo(Resultado.CHECKOUT_LIMPO);
    }

    @Test
    @DisplayName("corpo que não é JSON: ignorado, sem exceção")
    void jsonInvalido_ignorado() {
        assertThat(service.processar("isto nao e json".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo(Resultado.IGNORADO);
    }

    @Test
    @DisplayName("corpo vazio: ignorado, sem exceção")
    void corpoVazio_ignorado() {
        assertThat(service.processar(new byte[0])).isEqualTo(Resultado.IGNORADO);
    }
}
