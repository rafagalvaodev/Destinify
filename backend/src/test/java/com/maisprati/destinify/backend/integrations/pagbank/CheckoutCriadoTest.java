package com.maisprati.destinify.backend.integrations.pagbank;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CheckoutCriadoTest {

    private static final String URL_PAGAMENTO = "https://pagamento.pagbank.com.br/pagamento?code=XYZ";

    @Test
    void from_respostaCompleta_extraiIdELinkPay() {
        // Monta o mesmo formato do JSON do PagBank: a lista "links" tem vários itens; só o PAY nos interessa.
        Map<String, Object> resposta = Map.of(
                "id", "CHEC_123",
                "links", List.of(
                        Map.of("rel", "SELF", "href", "https://sandbox.api.pagseguro.com/checkouts/CHEC_123", "method", "GET"),
                        Map.of("rel", "PAY", "href", URL_PAGAMENTO, "method", "GET")));

        CheckoutCriado checkout = CheckoutCriado.from(resposta);

        assertThat(checkout.id()).isEqualTo("CHEC_123");
        assertThat(checkout.payUrl()).isEqualTo(URL_PAGAMENTO);
    }

    @Test
    void from_semLinkPay_lancaExcecao() {
        Map<String, Object> resposta = Map.of(
                "id", "CHEC_123",
                "links", List.of(Map.of("rel", "SELF", "href", "https://x")));

        assertThatThrownBy(() -> CheckoutCriado.from(resposta))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Resposta do PagBank sem o link PAY");
    }

    @Test
    void from_semLinks_lancaExcecao() {
        Map<String, Object> resposta = Map.of("id", "CHEC_123");

        assertThatThrownBy(() -> CheckoutCriado.from(resposta))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Resposta do PagBank sem a lista de links");
    }

    @Test
    void from_respostaNula_lancaExcecao() {
        assertThatThrownBy(() -> CheckoutCriado.from(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Resposta vazia do PagBank");
    }
}
