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

    private static final Logger log =
            LoggerFactory.getLogger(PagBankWebhookController.class);

    private final PagBankWebhookSignature signature;
    private final PagBankWebhookService webhookService;

    public PagBankWebhookController(
            PagBankWebhookSignature signature,
            PagBankWebhookService webhookService) {
        this.signature = signature;
        this.webhookService = webhookService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receber(
            @RequestBody byte[] corpo,
            @RequestHeader HttpHeaders cabecalhos) {

        try {
            List<String> assinaturas = cabecalhos.get("x-payload-signature");
            String authenticityToken = cabecalhos.getFirst("x-authenticity-token");
            boolean chavePublicaCarregada = signature.usaChavePublica();

            // Regra atual do Destinify: uma chave configurada ou a presença
            // do cabeçalho x-payload-signature seleciona a validação ECDSA.
            boolean modoEcdsa = chavePublicaCarregada || assinaturas != null;

            // A assinatura é verificada sobre os bytes originais recebidos.
            boolean autenticado = modoEcdsa
                    ? signature.validaAssinaturas(corpo, assinaturas)
                    : signature.valida(corpo, authenticityToken);

            if (!autenticado) {
                boolean temPayloadSignature = assinaturas != null
                        && assinaturas.stream().anyMatch(valor -> !valor.isBlank());

                boolean temAuthenticityToken = authenticityToken != null
                        && !authenticityToken.isBlank();

                // Registra a presença dos cabeçalhos sem expor seus valores.
                log.warn(
                        "PagBank webhook recusado | modo={} | chavePublicaCarregada={} "
                                + "| x-payload-signature={} | x-authenticity-token={} | bytes={}",
                        modoEcdsa ? "ECDSA" : "TOKEN",
                        chavePublicaCarregada,
                        temPayloadSignature,
                        temAuthenticityToken,
                        corpo.length
                );

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            PagBankWebhookService.Resultado resultado = webhookService.processar(corpo);
            log.info("PagBank webhook | resultado {}", resultado);
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.error("PagBank webhook | falha ao processar a notificação", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}