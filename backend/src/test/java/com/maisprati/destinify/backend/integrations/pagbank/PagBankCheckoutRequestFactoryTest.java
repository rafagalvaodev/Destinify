package com.maisprati.destinify.backend.integrations.pagbank;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PagBankCheckoutRequestFactoryTest {

    private static final String REDIRECT = "http://localhost:5173/pagamento/retorno";
    private static final String WEBHOOK = "https://abc123.ngrok.app/api/payments/pagbank/webhook";
    private static final ZonedDateTime EXPIRACAO =
            ZonedDateTime.of(2030, 1, 10, 12, 30, 0, 0, ZoneId.of("America/Sao_Paulo"));

    private PagBankCheckoutRequestFactory factory;
    private Payment payment;
    private Booking booking;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        factory = new PagBankCheckoutRequestFactory(REDIRECT, WEBHOOK);

        hotel = TestDataFactory.hotel(10L); // nome: "Hotel Central"

        booking = new Booking();
        booking.setId(7L);
        booking.setHotel(hotel);
        booking.setCheckInDate(LocalDate.of(2030, 3, 10));
        booking.setCheckOutDate(LocalDate.of(2030, 3, 13)); // 3 noites

        payment = new Payment();
        payment.setId(3L);
        payment.setAmount(new BigDecimal("350.50"));
    }

    // O JSON tem "items" como lista de objetos; este helper pega o primeiro item.
    @SuppressWarnings("unchecked")
    private Map<String, Object> primeiroItem(Map<String, Object> corpo) {
        return ((List<Map<String, Object>>) corpo.get("items")).get(0);
    }

    @Test
    void montar_identificaOPedidoComOIdDoPagamento() {
        Map<String, Object> corpo = factory.montar(payment, booking, EXPIRACAO);

        assertThat(corpo.get("reference_id")).isEqualTo("payment-3");
    }

    @Test
    void montar_expiracaoNoFormatoIso8601ComFuso() {
        Map<String, Object> corpo = factory.montar(payment, booking, EXPIRACAO);

        // São Paulo está em UTC-3 o ano todo (sem horário de verão desde 2019)
        assertThat(corpo.get("expiration_date")).isEqualTo("2030-01-10T12:30:00-03:00");
    }

    @Test
    void montar_enviaUmItemComAReservaEOValorEmCentavos() {
        Map<String, Object> item = primeiroItem(factory.montar(payment, booking, EXPIRACAO));

        assertThat(item.get("reference_id")).isEqualTo("booking-7");
        assertThat(item.get("name")).isEqualTo("Reserva - Hotel Central");
        assertThat(item.get("description").toString())
                .contains("10/03/2030")
                .contains("13/03/2030")
                .contains("3 noite(s)");
        assertThat(item.get("quantity")).isEqualTo(1);
        assertThat(item.get("unit_amount")).isEqualTo(35050); // R$ 350,50 em centavos
    }

    @Test
    void montar_nomeMuitoLongo_ficaLimitadoA100Caracteres() {
        hotel.setName("A".repeat(200));

        Map<String, Object> item = primeiroItem(factory.montar(payment, booking, EXPIRACAO));

        assertThat(item.get("name").toString()).hasSize(100);
    }

    @ParameterizedTest(name = "R$ {0} -> {1} centavos")
    @CsvSource({
            "350.50, 35050",
            "100, 10000",
            "0.01, 1",
            "1234.50, 123450",
            "100.125, 10013"   // 3 casas: arredonda para cima (HALF_UP), como no PaymentService
    })
    void emCentavos_converteSemErroDeArredondamento(BigDecimal valor, int centavos) {
        assertThat(PagBankCheckoutRequestFactory.emCentavos(valor)).isEqualTo(centavos);
    }

    @Test
    void montar_aceitaCreditoPixEBoleto_masNaoDebito() {
        Map<String, Object> corpo = factory.montar(payment, booking, EXPIRACAO);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> meios = (List<Map<String, String>>) corpo.get("payment_methods");

        assertThat(meios)
                .extracting(meio -> meio.get("type"))
                .containsExactly("CREDIT_CARD", "PIX", "BOLETO"); // DEBIT_CARD exige aprovação prévia
    }

    @Test
    void montar_naoEnviaCustomer_oPagBankPedeOsDadosNaPagina() {
        Map<String, Object> corpo = factory.montar(payment, booking, EXPIRACAO);

        assertThat(corpo).doesNotContainKey("customer");
    }

    @Test
    void montar_redirecionaParaOFrontComOIdDaReserva() {
        Map<String, Object> corpo = factory.montar(payment, booking, EXPIRACAO);

        assertThat(corpo.get("redirect_url")).isEqualTo(REDIRECT + "?reserva=7");
    }

    @Test
    void montar_comWebhookConfigurado_enviaAsUrlsDeNotificacao() {
        Map<String, Object> corpo = factory.montar(payment, booking, EXPIRACAO);

        assertThat(corpo.get("notification_urls")).isEqualTo(List.of(WEBHOOK));
        assertThat(corpo.get("payment_notification_urls")).isEqualTo(List.of(WEBHOOK));
    }

    @ParameterizedTest(name = "webhook [{0}] -> não envia as URLs")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void montar_semWebhook_naoEnviaAsUrlsDeNotificacao(String webhook) {
        // O PagBank exige de 5 a 100 caracteres; uma URL vazia seria recusada.
        PagBankCheckoutRequestFactory semWebhook = new PagBankCheckoutRequestFactory(REDIRECT, webhook);

        Map<String, Object> corpo = semWebhook.montar(payment, booking, EXPIRACAO);

        assertThat(corpo)
                .doesNotContainKey("notification_urls")
                .doesNotContainKey("payment_notification_urls");
    }
}
