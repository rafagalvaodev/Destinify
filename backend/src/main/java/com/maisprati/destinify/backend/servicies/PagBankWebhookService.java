package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.enums.GatewayOutcome;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankClient;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankNotification;
import com.maisprati.destinify.backend.integrations.pagbank.PagBankOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PagBankWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PagBankWebhookService.class);

    public enum Resultado {
        APROVADO, JA_PROCESSADO, NAO_PAGO, VALOR_DIVERGENTE, RESERVA_NAO_PENDENTE,
        PAGAMENTO_NAO_ENCONTRADO, CHECKOUT_LIMPO, IGNORADO
    }

    private final PagBankClient pagBankClient;
    private final PaymentConfirmationService confirmationService;

    public PagBankWebhookService(PagBankClient pagBankClient, PaymentConfirmationService confirmationService) {
        this.pagBankClient = pagBankClient;
        this.confirmationService = confirmationService;
    }

    public Resultado processar(byte[] corpo) {
        Map<String, Object> json;
        try {
            json = JsonParserFactory.getJsonParser().parseMap(new String(corpo, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            log.warn("PagBank webhook | copor não é um JSON válido : ignorado");
            return Resultado.IGNORADO;
        }
        return processar(json);
    }

    public Resultado processar(Map<?, ?> json) {
        PagBankNotification notificacao = PagBankNotification.from(json);
        log.info("PagBank webhook | tipo {} | id {} | status {}",
                notificacao.tipo(), notificacao.id(), notificacao.status());

        return switch (notificacao.tipo()) {
            case ORDER -> processarPedido(notificacao);
            case CHECKOUT -> processarCheckout(notificacao);
            default -> {
                log.info("PagBank webhook | tipo {} não tratado: ignorado", notificacao.tipo());
                yield Resultado.IGNORADO;
            }
        };
    }

    private Resultado processarPedido(PagBankNotification notificacao) {
        // Se a consulta falhar (rede, 5xx), a exceção sobe: o controller response 500 e o PagBank reenvia.
        PagBankOrder pedido = PagBankOrder.from(pagBankClient.consultarPedido(notificacao.id()));

        Long paymentId = PagBankNotification.paymentIdFrom(pedido.referenceId());
        if (paymentId == null) {
            paymentId = PagBankNotification.paymentIdFrom(notificacao.referenceId());
        }
        if (paymentId == null) {
            log.warn("PagBank webhook | pedido {} sem reference_id nosso: ignorado", notificacao.id());
            return Resultado.IGNORADO;
        }

        if (!pedido.pago()) {
            log.info("PagBank webhook | pagamento {} ainda não pago (status {})", paymentId, pedido.chargeStatus());
            return Resultado.NAO_PAGO;
        }

        GatewayOutcome resultado = confirmationService.approve(paymentId, pedido.paidCents());
        if (resultado == GatewayOutcome.AMOUNT_MISMATCH || resultado == GatewayOutcome.BOOKING_NOT_PENDING) {
            // Dinheiro recebido, mas não confirmamos: precisa de atenção humana (e de reembolso).
            log.error("PagBank webhook | ATENÇÃO pagamento {}: {}", paymentId, resultado);
        } else {
            log.info("PagBank webhook | pagamento {}: {}", paymentId, resultado);
        }
        return converter(resultado);
    }

    private Resultado processarCheckout(PagBankNotification notificacao) {
        boolean expirou = "EXPIRED".equals(notificacao.status()) || "INACTIVE".equals(notificacao.status());
        if (!expirou) {
            return Resultado.IGNORADO;
        }
        return confirmationService.clearExpiredCheckout(notificacao.id())
                ? Resultado.CHECKOUT_LIMPO
                : Resultado.IGNORADO;
    }

    private static Resultado converter(GatewayOutcome outcome) {
        return switch (outcome) {
            case APPROVED -> Resultado.APROVADO;
            case ALREADY_PROCESSED -> Resultado.JA_PROCESSADO;
            case AMOUNT_MISMATCH -> Resultado.VALOR_DIVERGENTE;
            case BOOKING_NOT_PENDING -> Resultado.RESERVA_NAO_PENDENTE;
            case NOT_FOUND -> Resultado.PAGAMENTO_NAO_ENCONTRADO;
        };
    }
}
