package com.maisprati.destinify.backend.integrations.pagbank;

import java.util.List;
import java.util.Map;

public record PagBankOrder(String referenceId, String chargeStatus, Integer paidCents) {

    public static PagBankOrder from(Map<?, ?> pedido) {
        if (pedido == null) {
            throw new IllegalStateException("Resposta vazia do PagBank");
        }

        String referenceId = PagBankNotification.texto(pedido.get("reference_id"));
        Map<?, ?> cobranca = escolherCobranca(pedido.get("charges"));

        if (cobranca == null) {
            return new PagBankOrder(referenceId, null, null);
        }

        return new PagBankOrder(
                referenceId,
                PagBankNotification.texto(cobranca.get("status")),
                valorEmCentavos(cobranca.get("amount")));
    }

    public boolean pago() {
        return "PAID".equals(chargeStatus) && paidCents != null;
    }

    private static Map<?, ?> escolherCobranca(Object charges) {
        if (!(charges instanceof List<?> lista)) {
            return null;
        }

        Map<?, ?> primeira = null;

        for (Object item : lista) {
            if (item instanceof Map<?, ?> cobranca) {
                if ("PAID".equals(cobranca.get("status"))) {
                    return cobranca;
                }

                if (primeira == null || "DECLINED".equals(primeira.get("status"))) {
                    primeira = cobranca;
                }
            }
        }

        return primeira;
    }

    private static Integer valorEmCentavos(Object amount) {
        if (!(amount instanceof Map<?, ?> valor)) {
            return null;
        }

        Integer pago = valor.get("summary") instanceof Map<?, ?> resumo
                ? numero(resumo.get("paid"))
                : null;

        // ALTERADO: zero é um valor informado e precisa ser preservado.
        // O serviço de confirmação compara esse valor com o total esperado.
        if (pago != null) {
            return pago;
        }

        return numero(valor.get("value"));
    }

    private static Integer numero(Object valor) {
        return valor instanceof Number n ? Integer.valueOf(n.intValue()) : null;
    }
}
