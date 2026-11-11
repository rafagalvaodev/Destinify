package com.maisprati.destinify.backend.integrations.pagbank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class PagBankWebhookSignature {

    private final byte[] token;

    public PagBankWebhookSignature(@Value("${pagbank.token}") String token) {
        this.token = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
    }

    // Calula o SHA-256 (hexadecimal minúsculo) de "{token}-{corpo}
    public String calcular(byte[] corpo) {
        try{
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            sha256.update(token);
            sha256.update((byte) '-');
            sha256.update(corpo);
            return HexFormat.of().formatHex(sha256.digest());
        } catch (NoSuchAlgorithmException e) {
            // Todo JDK é obritório a ter SHA-256; se faltar, o ambiente está quebrado
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /**
     * @param corpo                 os bytes do corpo exatamente como chegaram
     * @param assinaturaRecebida    o valor do cabeçalho x-authenticity-token (pode ser null)
     * @return true só se a assinatura recebida for igual à calculada
     */

    public boolean valida(byte[] corpo, String assinaturaRecebida) {
        if (token.length == 0 || corpo == null || assinaturaRecebida == null || assinaturaRecebida.isBlank()) {
            return false;
        }
        byte[] esperada = calcular(corpo).getBytes(StandardCharsets.US_ASCII);
        // trim/lowercase: tolera espaçoes e letras maiúsculas no cabeçalho, sem afrouxar a regra.
        byte[] recebida = assinaturaRecebida.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(esperada, recebida);
    }
}
