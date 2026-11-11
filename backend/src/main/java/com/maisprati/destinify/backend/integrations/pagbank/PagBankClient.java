package com.maisprati.destinify.backend.integrations.pagbank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * CLIENTE HTTP DO PAGBANK
 * Responsabilidade Única: "conversar com a API do PagBank". Esta classe não sabe nada de
 * reserva, usuário ou regra de negócio. Ela só sabe montar a requisição HTTP, enviar e devolver a resposta.
 * A regra de negócio fica no PaymentSerice.
 */

@Component
public class PagBankClient {

    private final RestClient restClient;

    public PagBankClient(RestClient.Builder restClientBuilder,
                         @Value("${pagbank.base-url}") String baseUrl,
                         @Value("${pagbank.token}") String token) {

        this.restClient = restClientBuilder.clone()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }

    public Map<?, ?> criarPedido(Map<String, Object> corpo) {
        return restClient.post()
                .uri("/orders")                           // baseUrl + "/orders"
                .contentType(MediaType.APPLICATION_JSON)      // avisa que o corpo é JSON
                .body(corpo)                                  // Map -> JSON autoomaticamente
                .retrieve()                                   // executa; lança exceção se der erro
                .body(Map.class);                              // JSON da resposta -> Map
    }

    public Map<?, ?> consultarPedido(String orderId) {
        return restClient.get()
                // {id} é um "template": o Spring troca pelo valor e escapa os caracteres.
                // Nunca monte URL concatenando texto ("/orders/" + id).
                .uri("/orders/{id}", orderId)
                .retrieve()
                .body(Map.class);
    }

    public Map<?, ?> criarCheckout(Map<String, Object> corpo) {
        return restClient.post()
                .uri("/checkouts")
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .body(Map.class);
    }

    public Map<?, ?> consultarCheckout(String checkoutId) {
        return restClient.get()
                .uri("/checkouts/{id}", checkoutId)
                .retrieve()
                .body(Map.class);
    }
}
