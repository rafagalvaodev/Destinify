package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.integrations.pagbank.PagBankWebhookSignature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public PagBankWebhookController(PagBankWebhookSignature signature) {
        this.signature = signature;
    }

    /**
     * @RequestBody byte[]: recebemos os BYTES crus do corpo (hash é calculado sobre eles).
     * @RequestHeader required=false: sem o cabeçalho a requisição não é rejeitada pelo Spring;
     * quem decidade é a validação da assinatura.
     */
    @PostMapping("/Webhook")
    public ResponseEntity<Void> receber(
            @RequestBody byte[] corpo,
            @RequestHeader(name = "x-authenticity-token", required = false) String assinaturaRecebida) {

        boolean valida = signature.valida(corpo, assinaturaRecebida);

        log.info("PagBank webhook | assinatura {} | cabeçalho presente: {} | {} bytes",
                valida ? "VÁLIDA" : "INVÁLIDA", assinaturaRecebida != null, corpo.length);

        if (!valida && assinaturaRecebida != null) {
            // Ajuda a depurar divergência (token errado? corpo alterado?) sem expor o hash inteiro.
            log.warn("PagBank webhook | recebida começa com {} | calculada começa com {}",
                    prefixo(assinaturaRecebida), prefixo(signature.calcular(corpo)));
        }

        log.info("PagBank webhook | corpo: {}", new String(corpo, StandardCharsets.UTF_8));

        return ResponseEntity.ok().build();
    }

    private static String prefixo(String hash) {
        String limpo = hash.trim();
        return limpo.length() <= 8 ? limpo : limpo.substring(0, 8) + "...";
    }
}
