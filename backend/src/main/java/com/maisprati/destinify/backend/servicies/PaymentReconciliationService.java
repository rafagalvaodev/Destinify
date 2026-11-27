package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Payment;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankClient;
import com.maisprati.destinify.backend.repositories.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(PaymentReconciliationService.class);
    private final PagBankClient client;
    private final PaymentRepository repository;
    private final PaymentConfirmationService confirmation;

    public enum Resultado {
        APROVADO, JA_PROCESSADO, NAO_PAGO, SEM_CHECKOUT, NAO_ENCONTRADO,
        DIVERGENCIA, RESERVA_NAO_PENDENTE
    }

    public PaymentReconciliationService(PagBankClient client, PaymentRepository repository,
                                        PaymentConfirmationService confirmation) {
        this.client = client;
        this.repository = repository;
        this.confirmation = confirmation;
    }

    public Resultado reconciliar(Long paymentId) {
        Payment payment = repository.findById(paymentId).orElse(null);
        if (payment == null) return Resultado.NAO_ENCONTRADO;
        if (payment.getPaymentStatus() == PaymentStatus.APPROVED) return Resultado.JA_PROCESSADO;
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) return Resultado.DIVERGENCIA;
        String checkoutId = payment.getProviderCheckoutId();
        if (checkoutId == null || checkoutId.isBlank()) return Resultado.SEM_CHECKOUT;
        String referencia = "payment-" + paymentId;

        // Nunca usa o status informado pelo navegador ou pelo corpo do webhook.
        Map<?, ?> checkout = client.consultarCheckout(checkoutId);
        if (checkout == null || !checkoutId.equals(checkout.get("id"))
                || !referencia.equals(checkout.get("reference_id"))) return Resultado.DIVERGENCIA;
        if (!(checkout.get("orders") instanceof List<?> pedidos)) return Resultado.DIVERGENCIA;

        // Uma pagina cheia pode ocultar outros pedidos; encaminhe para revisao.
        if (pedidos.size() >= 10) return Resultado.DIVERGENCIA;

        Integer pago = null;
        Set<String> ids = new HashSet<>();
        for (Object item : pedidos) {
            if (!(item instanceof Map<?, ?> pedido)
                    || !(pedido.get("id") instanceof String orderId)
                    || !orderId.startsWith("ORDE_") || !ids.add(orderId)) return Resultado.DIVERGENCIA;
            // Usa o endpoint fixo do cliente, não a URL fornecida pelo JSON.
            Map<?, ?> resposta = client.consultarPedido(orderId);
            if (resposta == null || !orderId.equals(resposta.get("id"))
                    || !referencia.equals(resposta.get("reference_id"))
                    || !(resposta.get("charges") instanceof List<?> cobrancas)) return Resultado.DIVERGENCIA;
            for (Object obj : cobrancas) {
                if (!(obj instanceof Map<?, ?> cobranca)) return Resultado.DIVERGENCIA;
                if (!"PAID".equals(cobranca.get("status"))) continue;
                // Mais de uma cobrança capturada requer revisão, não confirmação automática.
                if (pago != null
                        || !(cobranca.get("amount") instanceof Map<?, ?> valor)
                        || !"BRL".equals(valor.get("currency"))
                        || !(valor.get("summary") instanceof Map<?, ?> resumo)) return Resultado.DIVERGENCIA;
                Integer capturado = inteiro(resumo.get("paid"));
                Integer total = inteiro(resumo.get("total"));
                Integer estornado = inteiro(resumo.get("refunded"));
                if (capturado == null || capturado <= 0 || (resumo.containsKey("total") && !capturado.equals(total))
                        || !Integer.valueOf(0).equals(estornado)) return Resultado.DIVERGENCIA;
                pago = capturado;
            }
        }
        if (pago == null) return Resultado.NAO_PAGO;

        // A confirmação compara o valor com o banco e usa o bloqueio existente.
        var resultado = confirmation.approveForCheckout(paymentId, checkoutId, pago);
        log.info("PagBank reconciliacao | pagamento {} | {}", paymentId, resultado);
        return switch (resultado) {
            case APPROVED -> Resultado.APROVADO;
            case ALREADY_PROCESSED -> Resultado.JA_PROCESSADO;
            case AMOUNT_MISMATCH -> Resultado.DIVERGENCIA;
            case BOOKING_NOT_PENDING -> Resultado.RESERVA_NAO_PENDENTE;
            case NOT_FOUND -> Resultado.NAO_ENCONTRADO;
        };
    }

    // Não trunca números fracionários nem aceita overflow de int.
    private static Integer inteiro(Object valor) {
        if (!(valor instanceof Number numero)) return null;
        try { return new BigDecimal(numero.toString()).intValueExact(); }
        catch (ArithmeticException | NumberFormatException ex) { return null; }
    }
}