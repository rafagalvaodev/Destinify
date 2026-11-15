package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.integrations.pagbank.PagBankWebhookSignature;
import com.maisprati.destinify.backend.servicies.PagBankWebhookService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/payments/pagbank")
public class PagBankWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PagBankWebhookController.class);

    private final PagBankWebhookSignature signature;
    private final PagBankWebhookService webhookService;

    public PagBankWebhookController(PagBankWebhookSignature signature, PagBankWebhookService webhookService) {
        this.signature = signature;
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receber(
            @RequestBody byte[] corpo,
            @RequestHeader(name = "x-authenticity-token", required = false) String assinaturaRecebida) {

        if (!signature.valida(corpo, assinaturaRecebida)) {
            log.warn("PagBank webhook | assinatura inválida ou ausente: descartado (cabeçalho presente: {})",
                assinaturaRecebida != null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

        // só para a depuração no sanbox: corpo tem dados pessoais. Fica Desligado por padrão (nivel DEBUG).
        if (log.isDebugEnabled()) {
            log.debug("PagBank webhook | corpo: {}", new String(corpo, StandardCharsets.UTF_8));
        }

        try {
            PagBankWebhookService.Resultado resultado = webhookService.processar(corpo);
            log.info("PagBank webhook | resultado {}", resultado);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("PagBank webhook | falha ao processar; o PagBank deve reenviar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}