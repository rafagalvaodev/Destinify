package com.maisprati.destinify.backend.integrations.pagbank;

import java.util.List;
import java.util.Map;

public record CheckoutCriado(String id, String payUrl) {

    public static CheckoutCriado from(Map<?, ?> resposta) {

        // Lê a resposta do PagBank, se algo essencial faltar, lança IllegalStateException

        if (resposta == null) {
            throw new IllegalStateException("Resposta vazia do PagBank");
        }

        if (!(resposta.get("id") instanceof String checkoutId)) {
            throw new IllegalStateException("Resposta vazia do PagBank");
        }

        if (!(resposta.get("links") instanceof List<?> links)) {
            throw new IllegalStateException("Resposta do PagBank sem a lista de links");
        }

        // Procura, na lista, o link cujo "rel" é "PAY"
        for (Object item : links) {
            if(item instanceof Map<?, ?> link
            && "PAY".equals(link.get("rel"))
            && link.get("href") instanceof String href) {
                return new CheckoutCriado(checkoutId, href);
            }
        }
        throw new IllegalStateException("Resposta do PagBank sem o link PAY");
    }
}
