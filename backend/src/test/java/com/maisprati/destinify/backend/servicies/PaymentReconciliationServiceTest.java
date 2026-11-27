package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.enums.GatewayOutcome;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankClient;
import com.maisprati.destinify.backend.repositories.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.maisprati.destinify.backend.servicies.PaymentReconciliationService.Resultado.*;

class PaymentReconciliationServiceTest {
    private final PagBankClient client = mock(PagBankClient.class);
    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final PaymentConfirmationService confirmation = mock(PaymentConfirmationService.class);
    private final PaymentReconciliationService service = new PaymentReconciliationService(client, repository, confirmation);
    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = new Payment();
        payment.setId(3L);
        payment.setPaymentStatus(PaymentStatus.PENDING);
        payment.setProviderCheckoutId("CHEC_TESTE");
        when(repository.findById(3L)).thenReturn(Optional.of(payment));
    }

    private void checkout(String referencia, String status, List<?> pedidos) {
        doReturn(Map.of("id", "CHEC_TESTE", "reference_id", referencia,
                "status", status, "orders", pedidos)).when(client).consultarCheckout("CHEC_TESTE");
    }

    private Map<String, Object> cobranca(String status, String moeda, int pago, int estornado) {
        return Map.of("status", status,
                "amount", Map.of("currency", moeda, "summary", Map.of(
                        "paid", pago, "total", 88000, "refunded", estornado)));
    }

    private void pedido(List<?> cobrancas) {
        checkout("payment-3", "EXPIRED", List.of(Map.of("id", "ORDE_TESTE")));
        doReturn(Map.of("id", "ORDE_TESTE", "reference_id", "payment-3",
                "charges", cobrancas)).when(client).consultarPedido("ORDE_TESTE");
    }

    @Test
    void recuperaPagamentoAprovadoMesmoComCheckoutExpirado() {
        pedido(List.of(cobranca("PAID", "BRL", 88000, 0)));
        when(confirmation.approveForCheckout(3L, "CHEC_TESTE", 88000)).thenReturn(GatewayOutcome.APPROVED);
        assertEquals(APROVADO, service.reconciliar(3L));
        verify(confirmation).approveForCheckout(3L, "CHEC_TESTE", 88000);
    }

    @Test
    void pagamentoJaAprovadoNaoConsultaGateway() {
        payment.setPaymentStatus(PaymentStatus.APPROVED);
        assertEquals(JA_PROCESSADO, service.reconciliar(3L));
        verifyNoInteractions(client, confirmation);
    }

    @Test
    void referenciaDiferenteNaoConfirma() {
        checkout("payment-99", "EXPIRED", List.of());
        assertEquals(DIVERGENCIA, service.reconciliar(3L));
        verifyNoInteractions(confirmation);
    }

    @Test
    void checkoutAtivoSemPedidosContinuaPendente() {
        checkout("payment-3", "ACTIVE", List.of());
        assertEquals(NAO_PAGO, service.reconciliar(3L));
        verifyNoInteractions(confirmation);
    }

    @Test
    void cobrancaAguardandoNaoConfirmaNemEncerra() {
        pedido(List.of(cobranca("WAITING", "BRL", 0, 0)));
        assertEquals(NAO_PAGO, service.reconciliar(3L));
        verifyNoInteractions(confirmation);
    }

    @Test
    void moedaDiferenteNaoConfirma() {
        pedido(List.of(cobranca("PAID", "USD", 88000, 0)));
        assertEquals(DIVERGENCIA, service.reconciliar(3L));
        verifyNoInteractions(confirmation);
    }

    @Test
    void pagamentoEstornadoNaoConfirma() {
        pedido(List.of(cobranca("PAID", "BRL", 88000, 100)));
        assertEquals(DIVERGENCIA, service.reconciliar(3L));
        verifyNoInteractions(confirmation);
    }

    @Test
    void duasCobrancasPagasExigemRevisao() {
        pedido(List.of(cobranca("PAID", "BRL", 88000, 0), cobranca("PAID", "BRL", 88000, 0)));
        assertEquals(DIVERGENCIA, service.reconciliar(3L));
        verifyNoInteractions(confirmation);
    }

    @Test
    void valorDiferenteDoBancoNaoConfirma() {
        pedido(List.of(cobranca("PAID", "BRL", 88000, 0)));
        when(confirmation.approveForCheckout(3L, "CHEC_TESTE", 88000)).thenReturn(GatewayOutcome.AMOUNT_MISMATCH);
        assertEquals(DIVERGENCIA, service.reconciliar(3L));
    }
}