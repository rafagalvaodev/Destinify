package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.integrations.pagbank.PagBankWebhookSignature;
import com.maisprati.destinify.backend.servicies.PagBankWebhookService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            @RequestHeader HttpHeaders cabecalhos) {

        try {
            List<String> assinaturas = cabecalhos.get("x-payload-signature");

            boolean modoEcdsa = signature.usaChavePublica() || assinaturas != null;
            boolean autenticado = modoEcdsa
                    ? signature.validaAssinaturas(corpo, assinaturas)
                    : signature.valida(corpo, cabecalhos.getFirst("x-authenticity-token"));

            if (!autenticado) {
                log.warn("PagBank webhook | assinatura válida inválida ou ausente");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            PagBankWebhookService.Resultado resultado = webhookService.processar(corpo);
            log.info("PagBank webook | resultado {}", resultado);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("PagBank webhook | falha ao processar; o PagBank deve reenviar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}