package com.maisprati.destinify.backend.integrations.pagbank;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Component
public class PagBankWebhookSignature {

    private final byte[] token;
    private final PublicKey chavePublica;

    public PagBankWebhookSignature(
            @Value("${pagbank.token}") String token,
            @Value("${pagbank.webhook-public-key:}") String chavePublicaBase64) {
        this.token = token == null ? new byte[0] : token.getBytes(StandardCharsets.UTF_8);
        this.chavePublica = lerChavePublica(chavePublicaBase64);
    }

    private static PublicKey lerChavePublica(String chaveBase64) {
        if (chaveBase64 == null || chaveBase64.isBlank()) {
            return null;
        }
        try {
            byte[] chave = Base64.getDecoder().decode(chaveBase64.trim());
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(chave));
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Chave públic de webhook do PagBank inválida", ex);
        }
    }

    public boolean usaChavePublica() {
        return chavePublica != null;
    }

    public boolean validaAssinaturas(byte[] corpo, List<String> assinaturas) {
        if (chavePublica == null || corpo == null || assinaturas == null) {
            return false;
        }

        for (String cabecalho : assinaturas) {
            for (String valor : cabecalho.split(",")) {
                if (validaEcdsa(corpo, valor.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean validaEcdsa(byte[] corpo, String assinaturaBase64) {
        try {
            byte[] assinatura = Base64.getDecoder().decode(assinaturaBase64);
            Signature verificador = Signature.getInstance("SHA256withECDSA");
            verificador.initVerify(chavePublica);
            verificador.update(corpo);
            return verificador.verify(assinatura);
        } catch (SignatureException | IllegalArgumentException ex) {
            // uma assinatura mal formada pode vir junto com outra válida
            return false;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Não foi possível verificar a assinatura do PagBank", ex);
        }
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
