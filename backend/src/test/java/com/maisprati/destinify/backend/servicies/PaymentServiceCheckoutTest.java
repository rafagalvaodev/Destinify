package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.CheckoutResponse;
import com.maisprati.destinify.backend.domain.dto.PaymentDTO.PaymentResponse;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.exceptions.ForbiddenException;
import com.maisprati.destinify.backend.exceptions.PaymentGatewayException;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankCheckoutRequestFactory;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankClient;
import com.maisprati.destinify.backend.repositories.BookingRepository;
import com.maisprati.destinify.backend.repositories.PaymentRepository;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.utils.mappers.PaymentMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService.startCheckout")
class PaymentServiceCheckoutTest {

    private static final String URL_NOVA = "https://pagamento.pagbank.com.br/pagamento?code=NOVO";
    private static final String URL_ANTIGA = "https://pagamento.pagbank.com.br/pagamento?code=ANTIGO";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private PagBankClient pagBankClient;

    @Mock
    private PagBankCheckoutRequestFactory checkoutRequestFactory;

    @InjectMocks
    private PaymentService paymentService;

    private User owner;
    private Booking booking;
    private Payment payment;
    private PaymentResponse paymentResponse;
    private Map<String, Object> corpo;
    private Map<String, Object> respostaPagBank;

    @BeforeEach
    void setUp() {
        // O campo @Value é privado e, sem Spring, ficaria 0. Definimos 30 minutos.
        ReflectionTestUtils.setField(paymentService, "checkoutExpirationMinutes", 30L);

        owner = TestDataFactory.client(1L);

        booking = new Booking();
        booking.setId(7L);
        booking.setUser(owner);
        booking.setHotel(TestDataFactory.hotel(10L));
        booking.setStatus(BookingStatus.PENDING);
        booking.setCheckInDate(LocalDate.now().plusDays(10));
        booking.setCheckOutDate(LocalDate.now().plusDays(13));
        booking.setTotalPrice(new BigDecimal("300.00"));

        payment = new Payment(3L, booking, new BigDecimal("300.00"),
                PaymentStatus.PENDING, LocalDateTime.now(), null);
        paymentResponse = new PaymentResponse(3L, 7L, new BigDecimal("300.00"),
                PaymentStatus.PENDING, LocalDateTime.now(), null);

        corpo = Map.of("reference_id", "payment-3");
        respostaPagBank = Map.of(
                "id", "CHEC_NOVO",
                "links", List.of(Map.of("rel", "PAY", "href", URL_NOVA)));
    }

    // Cenário base: a reserva é de outro usuário, está PENDING e já existe um pagamento PENDING.
    private void stubPagamentoPendenteExistente() {
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(7L, PaymentStatus.PENDING))
                .thenReturn(Optional.of(payment));
        when(paymentMapper.paymentResponseMapper(payment)).thenReturn(paymentResponse);
        when(paymentRepository.findById(3L)).thenReturn(Optional.of(payment));
    }

    private void stubCriacaoDoCheckoutNoPagBank() {
        when(checkoutRequestFactory.montar(eq(payment), eq(booking), any(ZonedDateTime.class))).thenReturn(corpo);
        doReturn(respostaPagBank).when(pagBankClient).criarCheckout(corpo);
    }

    @Test
    @DisplayName("sem checkout: cria no PagBank, guarda id/link/validade e devolve o link")
    void semCheckout_criaEGuarda() {
        stubPagamentoPendenteExistente();
        stubCriacaoDoCheckoutNoPagBank();

        CheckoutResponse result = paymentService.startCheckout(7L, owner);

        assertThat(result.paymentId()).isEqualTo(3L);
        assertThat(result.bookingId()).isEqualTo(7L);
        assertThat(result.checkoutUrl()).isEqualTo(URL_NOVA);

        assertThat(payment.getProviderCheckoutId()).isEqualTo("CHEC_NOVO");
        assertThat(payment.getCheckoutUrl()).isEqualTo(URL_NOVA);

        // validade = agora (fuso horário de São Paulo) + 30 minutos, com 1 minuto de margem
        LocalDateTime agora = LocalDateTime.now(PaymentService.ZONA);
        assertThat(payment.getExpiresAt()).isBetween(agora.plusMinutes(29), agora.plusMinutes(31));

        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("checkout ainda válido: devolve o mesmo link e não chama o PagBank")
    void checkoutVigente_reaproveita() {
        payment.setProviderCheckoutId("CHEC_ANTIGO");
        payment.setCheckoutUrl(URL_ANTIGA);
        payment.setExpiresAt(LocalDateTime.now(PaymentService.ZONA).plusMinutes(10));
        stubPagamentoPendenteExistente();

        CheckoutResponse result = paymentService.startCheckout(7L, owner);

        assertThat(result.checkoutUrl()).isEqualTo(URL_ANTIGA);
        verify(pagBankClient, never()).criarCheckout(any());
        verify(checkoutRequestFactory, never()).montar(any(), any(), any());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("checkout vencido: cria um novo e substitui os dados antigos")
    void checkoutVencido_criaNovo() {
        payment.setProviderCheckoutId("CHEC_ANTIGO");
        payment.setCheckoutUrl(URL_ANTIGA);
        payment.setExpiresAt(LocalDateTime.now(PaymentService.ZONA).minusDays(5)); // já venceu
        stubPagamentoPendenteExistente();
        stubCriacaoDoCheckoutNoPagBank();

        CheckoutResponse result = paymentService.startCheckout(7L, owner);

        assertThat(result.checkoutUrl()).isEqualTo(URL_NOVA);
        assertThat(payment.getProviderCheckoutId()).isEqualTo("CHEC_NOVO");
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("PagBank response erro(ex.: 401): lança PaymentGatewayException e não salva nada")
    void pagBankComErro_lancaGatewayException() {
        stubPagamentoPendenteExistente();
        when(checkoutRequestFactory.montar(eq(payment), eq(booking), any(ZonedDateTime.class))).thenReturn(corpo);
        when(pagBankClient.criarCheckout(corpo)).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> paymentService.startCheckout(7L, owner))
                .isInstanceOf(PaymentGatewayException.class)
                .hasMessage("Não foi possível criar o checkout no PagBank")
                .hasCauseInstanceOf(HttpClientErrorException.class);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("PagBank responde sem o link PAY: lança PaymentGatewayException")
    void respostaSemLinkPay_lancaGatewayException() {
        stubPagamentoPendenteExistente();
        when(checkoutRequestFactory.montar(eq(payment), eq(booking), any(ZonedDateTime.class))).thenReturn(corpo);
        doReturn(Map.of("id", "CHEC_X", "links", List.of())).when(pagBankClient).criarCheckout(corpo);

        assertThatThrownBy(() -> paymentService.startCheckout(7L, owner))
                .isInstanceOf(PaymentGatewayException.class)
                .hasCauseInstanceOf(IllegalStateException.class);

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    @DisplayName("reserva de outro usuário: 403 e o PagBank nem é chamado")
    void reservaDeOutroUsuario_lancaForbidden() {
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> paymentService.startCheckout(7L, TestDataFactory.client(2L)))
                .isInstanceOf(ForbiddenException.class);

        verify(pagBankClient, never()).criarCheckout(any());
    }
}
