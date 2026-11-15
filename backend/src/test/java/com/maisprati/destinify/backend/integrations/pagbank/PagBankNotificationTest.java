package com.maisprati.destinify.backend.integrations.pagbank;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.support.TestDataFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PagBankNotificationTest {

    @ParameterizedTest(name = "id {0} -> tipo {1}")
    @CsvSource({
            "ORDE_123, ORDER",
            "CHEC_123, CHECKOUT",
            "CHAR_123, CHARGE",
            "XPTO_123, DESCONHECIDO"
    })

    void from_identificaOTipoPeloPrefixoDoId(String id, PagBankNotification.Tipo esperado) {
        PagBankNotification notificacao = PagBankNotification.from(Map.of("id", id));

        assertThat(notificacao.tipo()).isEqualTo(esperado);
        assertThat(notificacao.id()).isEqualTo(id);
    }

    @Test
    void from_lePedidoComReferenciaEStatus() {
        PagBankNotification notificacao = PagBankNotification.from(
                Map.of("id", "ORDE_1", "reference_id", "payment-3", "status", "PAID"));
    }

    @Test
    void from_semId_ehDesconhecido() {
        assertThat(PagBankNotification.from(Map.of("status", "PAID")).tipo())
                .isEqualTo(PagBankNotification.Tipo.DESCONHECIDO);
    }

    @Test
    void from_jsonNulo_ehDeconhecido() {
        assertThat(PagBankNotification.from(null).tipo()).isEqualTo(PagBankNotification.Tipo.DESCONHECIDO);
    }

    @Test
    void from_idQueNaoEhTexto_ehDesconhecido() {
        Map<String, Object> json = new HashMap<>();
        json.put("id", 12345); // número, e não texto

        assertThat(PagBankNotification.from(json).tipo()).isEqualTo(PagBankNotification.Tipo.DESCONHECIDO);
    }

    @ParameterizedTest(name = "[{0}] -> {1}")
    @CsvSource({
            "payment-3, 3",
            "payment-1234567, 1234567",
            "'  payment-8  ', 8"
    })

    void paymentIdFrom_extraiOIdDoPagamento(String referencia, Long esperado) {
        assertThat(PagBankNotification.paymentIdFrom(referencia)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "[{0}] não é um pagametno nosso")
    @NullAndEmptySource
    @ValueSource(strings = {"booking-7", "payment-", "payment-abc", "payment-3x", "xpayment-3",
            "payment--3", "payment-99999999999999999999"})

    void paymentIdFrom_formatoInvalido_retornaNull(String referencia) {
        assertThat(PagBankNotification.paymentIdFrom(referencia)).isNull();
    }

    @Test
    void contrato_oReferenceIdQueAFactoryEscreveEhOQueOParserLe() {
        PagBankCheckoutRequestFactory factory = new PagBankCheckoutRequestFactory("http://localhost:5173/retorno", "");
        Booking booking = new Booking();
        booking.setId(7L);
        booking.setHotel(TestDataFactory.hotel(10L));
        booking.setCheckInDate(LocalDate.of(2030, 3, 10));
        booking.setCheckOutDate(LocalDate.of(2030, 3, 13));
        Payment payment = new Payment();
        payment.setId(42L);
        payment.setAmount(new BigDecimal("300.00"));

        Map<String, Object> corpo = factory.montar(payment, booking,
                ZonedDateTime.of(2030, 1, 10, 12, 30, 0, 0, ZoneId.of("America/Sao_Paulo")));

        assertThat(PagBankNotification.paymentIdFrom((String) corpo.get("reference_id"))).isEqualTo(42L);
    }

}
