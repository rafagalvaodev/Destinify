package com.maisprati.destinify.backend.integrations.pagbank;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PagBankCheckoutRequestFactory {

    private static final List<String> MEIOS_DE_PAGAMENTO = List.of("CREDIT_CARD", "PIX", "BOLETO");

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/mm/yyyy");

    private final String redirectUrl;
    private final String notificationUrl;

    public PagBankCheckoutRequestFactory(
            @Value("${pagbank.redirect-url:http//localhost:5173/pagamento/retorno}") String redirectUrl,
            @Value("${pagbank.notification-url:}") String notificationUrl) {
        this.redirectUrl = redirectUrl;
        this.notificationUrl = notificationUrl;
    }

    // @param expiracao momento em que o checkout deixa de valer (o service calcula; recebê-lo
    //      como parâmetro deixa esta classe "pura", sem depender do relógio)

    public Map<String, Object> montar(Payment payment, Booking booking, ZonedDateTime expiracao) {
        // LinkedHashMap mantém a ordem de inserção: o JSON sai na ordem que escremos
        Map<String, Object> corpo = new LinkedHashMap<>();

        // Identificador nosso para este pedido (máx. 64 caracteres). Facilita achar depois
        corpo.put("reference_id", "payment-" + payment.getId());

        // ISO-8601 com fuso, sem frações de segundo
        corpo.put("expiration_date", expiracao
                .truncatedTo(ChronoUnit.SECONDS)
                .toOffsetDateTime()
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        corpo.put("items", List.of(montarItem(payment, booking)));

        // Cada meio vira {"type":"PIX"}, {"type":"BOLETO}...
        corpo.put("payment_methods", MEIOS_DE_PAGAMENTO.stream()
                .map(tipo -> Map.of("type", tipo))
                .toList());

        // Volta para o front já dizendo qual reserva foi paga, para a tela de retorno consultar o status.
        corpo.put("redirect_url", redirectUrl + "?reserva=" + booking.getId());

        if (notificationUrl != null && !notificationUrl.isBlank()) {
            corpo.put("notification_urls", List.of(notificationUrl));
            corpo.put("payment_notifications_urls", List.of(notificationUrl));
        }
        return corpo;
    }

    private Map<String, Object> montarItem(Payment payment, Booking booking) {
        long noites = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("reference_id", "booking-" + booking.getId());
        item.put("name", limitar("Reserva - " + booking.getHotel().getName(), 100));
        item.put("description", limitar("Check-in " + DATA_BR.format(booking.getCheckInDate())
            + " | Check-out " + DATA_BR.format(booking.getCheckOutDate())
            + " | " + noites + " noite(s)", 225));
        item.put("quantity", 1);
        item.put("unit_amount", emCentavos(payment.getAmount()));
        return item;
    }

    /**
     * R$ 350,50 -> 35050. Usa BigDecimal (nunca double) para não ter erro de arredondamento.
     * movePointRight(2) "anda" a vírgula duas casas; intValueExact() garante que o resultado é
     * inteiro e cabe em int (senão lança exceção em vez de enviar um valor errado).
     */

    public static int emCentavos(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).movePointRight(2).intValueExact();
    }

    // O PagBank limita o tamanho dos textos (nome: 100, descrição 255). Cortamos em vez de errar.
    private static String limitar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo);
    }
}
