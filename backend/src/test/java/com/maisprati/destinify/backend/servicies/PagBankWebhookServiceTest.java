package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.enums.GatewayOutcome;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankClient;
import com.maisprati.destinify.backend.servicies.PagBankWebhookService.Resultado;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagBankWebhookService")
class PagBankWebhookServiceTest {

    @Mock
    private PagBankClient pagBankClient;

    @Mock
    private PaymentConfirmationService confirmationService;

    @InjectMocks
    private PagBankWebhookService service;

    private static Map<String, Object> notificacaoDePedido() {
        return Map.of("id", "ORDE_1", "reference_id", "payment-3", "status", "PAID");
    }

    private static Map<String, Object> pedidoNoPagBank(String referencia, String statusDaCobranca, int centavos) {
        return Map.of("id", "ORDE_1", "reference_id", referencia, "charges", List.of(
                Map.of("id", "CHAR_1", "status", statusDaCobranca,
                        "amount", Map.of("value", centavos, "summary", Map.of("paid", centavos)))));
    }

    @Test
    @DisplayName("pedido pago: consulta o PagBank e aprova o pagamento com o valor VERIFICADO")
    void pedidoPago_aprova() {
        doReturn(pedidoNoPagBank("payment-3", "PAID", 30000)).when(pagBankClient).consultarPedido("ORDE_1");
        when(confirmationService.approve(3L, 30000)).thenReturn(GatewayOutcome.APPROVED);

        assertThat(service.processar(notificacaoDePedido())).isEqualTo(Resultado.APROVADO);

        verify(confirmationService).approve(3L, 30000);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "APPROVED, APROVADO",
            "ALREADY_PROCESSED, JA_PROCESSADO",
            "AMOUNT_MISMATCH, VALOR_DIVERGENTE",
            "BOOKING_NOT_PENDING, RESERVA_NAO_PENDENTE",
            "NOT_FOUND, PAGAMENTO_NAO_ENCONTRADO"
    })

    @DisplayName("traduz cada resultado da confrimação")
    void traduzOResultadoDaConfirmacao(GatewayOutcome outcome, Resultado esperado) {
        doReturn(pedidoNoPagBank("payment-3", "PAID", 30000)).when(pagBankClient).consultarPedido("ORDE_1");
        when(confirmationService.approve(3L, 30000)).thenReturn(outcome);

        assertThat(service.processar(notificacaoDePedido())).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Segurança: não confia no corpo da notificação; vale o que o PagBank diz na consulta")
    void notificacaoDizPaga_masOPagBankDizAguardando_naoAprova() {
        doReturn(pedidoNoPagBank("payment-3", "WAITING", 30000)).when(pagBankClient).consultarPedido("ORDE_1");

        assertThat(service.processar(notificacaoDePedido())).isEqualTo(Resultado.NAO_PAGO);

        verify(confirmationService, never()).approve(anyLong(), anyInt());
    }

    @Test
    @DisplayName("usa o reference_id do pedido consultado quando a notificação não traz")
    void notificacaoSemReferencia_usaAReferenciaDoPedidoConsultado() {
        doReturn(pedidoNoPagBank("payment-3", "PAID", 30000)).when(pagBankClient).consultarPedido("ORDE_1");
        when(confirmationService.approve(3L, 30000)).thenReturn(GatewayOutcome.APPROVED);

        assertThat(service.processar(Map.of("id", "ORDE_1"))).isEqualTo(Resultado.APROVADO);
    }

    @Test
    @DisplayName("pedido que não é nosso (sem 'payment-N'): ignorado")
    void pedidoSemReferenciaNossa_ignorado() {
        doReturn(pedidoNoPagBank("outro-sistema-99", "PAID", 30000)).when(pagBankClient).consultarPedido("ORDE_1");

        assertThat(service.processar(Map.of("id", "ORDE_1"))).isEqualTo(Resultado.IGNORADO);

        verify(confirmationService, never()).approve(anyLong(), anyInt());
    }

    @Test
    @DisplayName("falha ao consultar o PagBank: a exceção sobe (o controller response 500 e o PagBank reenvia)")
    void falhaNaConsulta_propagaAExcecao() {
        when(pagBankClient.consultarPedido("ORDE_1")).thenThrow(new IllegalStateException("PagBank fora do ar"));

        assertThatThrownBy(() -> service.processar(notificacaoDePedido()))
                .isInstanceOf(IllegalStateException.class);

        verify(confirmationService, never()).approve(anyLong(), anyInt());
    }

    @Test
    @DisplayName("checkout EXPIRED: limpa os dados do checkout no pagamento")
    void checkoutExpirado_limpa() {
        when(confirmationService.clearExpiredCheckout("CHEC_1")).thenReturn(true);

        assertThat(service.processar(Map.of("id", "CHEC_1", "status", "EXPIRED")))
                .isEqualTo(Resultado.CHECKOUT_LIMPO);
    }

    @Test
    @DisplayName("checkout expirado de um pagamento que não existe/já foi processado: ignorado")
    void checkoutExpirado_semPagamentoPendente_ignorado() {
        when(confirmationService.clearExpiredCheckout ("CHEC_1")).thenReturn(false);

        assertThat(service.processar(Map.of("id", "CHEC_1", "status", "INACTIVE")))
                .isEqualTo(Resultado.IGNORADO);
    }

    @Test
    @DisplayName("checkout com outro status (ex.: ACTIVE): ignorado nada é limpo")
    void checkoutAtivo_ignorado() {
        assertThat(service.processar(Map.of("id", "CHEC_1", "status", "ACTIVE"))).isEqualTo(Resultado.IGNORADO);

        verify(confirmationService, never()).clearExpiredCheckout(anyString());
    }

    @ParameterizedTest(name = "id {0} é ignorado")
    @CsvSource({"CHAR_1", "XPTO_1"})
    @DisplayName("tipos que não tratamos: ignorado, sem consultar o PagBank")
    void tiposNaoTratados_ignorados(String id) {
        assertThat(service.processar(Map.of("id", id, "status", "PAID"))).isEqualTo(Resultado.IGNORADO);

        verify(pagBankClient, never()).consultarPedido(any());
        verify(confirmationService, never()).approve(anyLong(), anyInt());
    }
}
