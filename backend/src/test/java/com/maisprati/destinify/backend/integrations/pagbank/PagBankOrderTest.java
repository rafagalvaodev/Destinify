package com.maisprati.destinify.backend.integrations.pagbank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PagBankOrderTest {

    private static Map<String, Object> cobranca(String status, int valor, Integer pago) {
        Map<String, Object> amount = pago == null
                ? Map.of("value", valor, "currency", "BRL")
                : Map.of("value", valor, "currency", "BRL",
                "summary", Map.of("total", valor, "paid", pago, "refunded", 0));
        return Map.of("id", "CHAR_1", "status", status, "amount", amount);
    }

    private static Map<String, Object> pedido(String referencia, Object charges) {
        return Map.of("id", "ORDE_1", "reference_id", referencia, "charges", charges);
    }

    @Test
    void from_cobrancaPaga_usaOValorPagoDoResumo() {
        PagBankOrder order = PagBankOrder.from(pedido("payment-3", List.of(cobranca("PAID", 30000, 30000))));

        assertThat(order.referenceId()).isEqualTo("payment-3");
        assertThat(order.chargeStatus()).isEqualTo("PAID");
        assertThat(order.paidCents()).isEqualTo(30000);
        assertThat(order.pago()).isTrue();
    }

    @Test
    void from_semResumoDoPagamento_usaOValorDaCobranca() {
        PagBankOrder order = PagBankOrder.from(pedido("payment-3", List.of(cobranca("PAID", 30000, null))));

        assertThat(order.paidCents()).isEqualTo(30000);
        assertThat(order.pago()).isTrue();
    }

    @Test
    void from_resumoComPagoZero_usaOValorDaCobranca() {
        PagBankOrder order = PagBankOrder.from(pedido("payment-3", List.of(cobranca("PAID", 30000, 0))));

        assertThat(order.paidCents()).isEqualTo(30000);
    }

    @Test
    void from_preferiACobrancaPagaQuandoHaVarias() {
        // O cliente errou o cartão (DECLINED) e depois acertou (PAID) -> vale a paga
        PagBankOrder order = PagBankOrder.from(pedido("paytment-3",
                List.of(cobranca("DECLINED", 30000, null), cobranca("PAID", 30000, 30000))));

        assertThat(order.pago()).isTrue();
    }

    @Test
    void from_cobrancaNaoPaga_naoEstaPago() {
        PagBankOrder order = PagBankOrder.from(pedido("payment-3", List.of(cobranca("WAITING", 30000, null))));

        assertThat(order.chargeStatus()).isEqualTo("WAITING");
        assertThat(order.pago()).isFalse();
    }

    @Test
    void from_pedidoSemCobrancas_naoEstaPago() {
        PagBankOrder order = PagBankOrder.from(pedido("payment-3", List.of()));

        assertThat(order.chargeStatus()).isNull();
        assertThat(order.pago()).isFalse();
    }

    @Test
    void from_semListaDeCobrancas_naoEstaPago() {
        PagBankOrder order = PagBankOrder.from(Map.of("id", "ORDE_1", "reference_id", "payment-3"));

        assertThat(order.pago()).isFalse();
        assertThat(order.referenceId()).isEqualTo("payment-3");
    }

    @Test
    void from_pagoMasSemValor_naoEstaPago() {
        // Status PAID, mas não sabemos quanto: por segurança, não consideramos pago
        PagBankOrder order = PagBankOrder.from(
                pedido("payment-3", List.of("id", "CHAR_1", "status", "PAID")));

        assertThat(order.pago()).isFalse();
    }

    @Test
    void from_formatoInesperado_naoEstaPagoENaoLancaExcecao() {
        PagBankOrder order = PagBankOrder.from(Map.of("charges", "isso-nao-e-uma-lista", "reference_id", 123));

        assertThat(order.pago()).isFalse();
        assertThat(order.referenceId()).isNull();
    }

    @Test
    void from_respostaNula_lancaExcecao() {
        assertThatThrownBy(() -> PagBankOrder.from(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
