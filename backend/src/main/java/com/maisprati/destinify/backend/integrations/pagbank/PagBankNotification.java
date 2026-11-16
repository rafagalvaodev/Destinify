package com.maisprati.destinify.backend.integrations.pagbank;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public record PagBankNotification(Tipo tipo, String id, String referenceId,String status) {

    public enum Tipo { ORDER, CHECKOUT, CHARGE, DESCONHECIDO }

    public static final String PREFIXO_REFERENCIA = "payment-";

    private static final Pattern REFERENCIA = Pattern.compile("^" + PREFIXO_REFERENCIA + "(\\d{1,18})$");

    public static PagBankNotification from(Map<?, ?> json) {
        if (json == null) {
            return new PagBankNotification(Tipo.DESCONHECIDO, null, null, null);
        }
        String id = texto(json.get("id"));
        return new PagBankNotification(tipoDe(id), id, texto(json.get("reference_id")), texto(json.get("status")));
    }

    public static Long paymentIdFrom(String referenceId) {
        if (referenceId == null) {
            return null;
        }
        Matcher matcher = REFERENCIA.matcher(referenceId.trim());
        return matcher.matches() ? Long.valueOf(matcher.group(1)) : null;
    }

    public static Tipo tipoDe(String id) {
        if (id == null) return Tipo.DESCONHECIDO;
        if (id.startsWith("ORDE_")) return Tipo.ORDER;
        if (id.startsWith("CHEC_")) return Tipo.CHECKOUT;
        if (id.startsWith("CHAR_")) return Tipo.CHARGE;
        return Tipo.DESCONHECIDO;
    }

    static String texto(Object valor) {
        return valor instanceof String s && !s.isBlank() ? s : null;
    }


}
