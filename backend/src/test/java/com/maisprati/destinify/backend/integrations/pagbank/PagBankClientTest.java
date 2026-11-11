package com.maisprati.destinify.backend.integrations.pagbank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PagBankClientTest {

    private static final String BASE_URL = "https://sandbox.api.pagseguro.com";

    private MockRestServiceServer server;
    private PagBankClient client;

    @BeforeEach
    void setUp() {
         RestClient.Builder builder = RestClient.builder();

         server = MockRestServiceServer.bindTo(builder).build();

         client = new PagBankClient(builder, BASE_URL, "token-de-teste");
    }

    @Test
    void criarPedido_enviaPostComBearerEJson_eDevolveOCorpo() {
        server.expect(requestTo(BASE_URL + "/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-de-teste"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reference_id").value("pagamento-1")) // $ = raiz do JSON enviado
                .andRespond(withSuccess("{\"id\":\"ORDE_123\"}", MediaType.APPLICATION_JSON));

        // Act
        Map<?, ?> resposta = client.criarPedido(Map.of("reference_id", "pagamento-1"));

        assertThat(resposta.get("id")).isEqualTo("ORDE_123");
        server.verify();
    }

    @Test
    void consultarPedido_enviaGetNaUrlDoPedido() {
        server.expect(requestTo(BASE_URL + "/orders/ORDE_123")) // prova que o {id} foi trocado
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-teste"))
                .andRespond(withSuccess("{\"id\":\"ORDE_123\"}", MediaType.APPLICATION_JSON));

        Map<?, ?> resposta = client.consultarPedido("ORDE_123");

        assertThat(resposta.get("id")).isEqualTo("ORDE_123");
        server.verify();
    }

    @Test
    void criarPedido_tokenInvalido_lancaUnauthorized() {
        // O PagBank falso response 401. O retrieve() transforma isso em exceção.
        server.expect(requestTo(BASE_URL + "/orders"))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.criarPedido(Map.of("reference_id", "x")))
                .isInstanceOf(HttpClientErrorException.Unauthorized.class);
    }

    @Test
    void consultarPedido_pedidoInexistente_lancaNotFound() {
        server.expect(requestTo(BASE_URL + "/orders/ORDE_123"))
                .andRespond(withResourceNotFound());

        assertThatThrownBy(() -> client.consultarPedido("ORDE_123"))
                .isInstanceOf(HttpClientErrorException.NotFound.class);
    }

    @Test
    void criarCheckout_enviaPostEmCheckouts_eDevolveOCorpo() {
        server.expect(requestTo(BASE_URL + "/checkouts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer token-de-teste"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reference_id").value("payment-1"))
                .andRespond(withSuccess("{\"id\":\"CHEC_123\"}", MediaType.APPLICATION_JSON));

        Map<?, ?> resposta = client.criarCheckout(Map.of("reference_id", "payment-1"));

        assertThat(resposta.get("id")).isEqualTo("CHEC_123");
        server.verify();
    }

    @Test
    void consultarCheckout_enviaGetNaUrlDoCheckout() {
        server.expect(requestTo(BASE_URL + "/checkouts/CHEC_123"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-teste"))
                .andRespond(withSuccess("{\"id\":\"CHEC_123\",\"status\":\"ACTIVATE\"}", MediaType.APPLICATION_JSON));

        Map<?, ?> resposta = client.consultarCheckout("CHEC_123");

        assertThat(resposta.get("status")).isEqualTo("ACTIVATE");
        server.verify();
    }

    @Test
    void criarCheckout_corpoInvalido_lancaBadRequest() {
        // O PagBank devolve 400 quando falta campo obrigatório ou o formato está errado
        server.expect(requestTo(BASE_URL + "/checkouts"))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> client.criarCheckout(Map.of("reference_id", "x")))
                .isInstanceOf(HttpClientErrorException.BadRequest.class);
    }
}
