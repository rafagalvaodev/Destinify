package com.maisprati.destinify.backend.integrations.pagbank;

import com.maisprati.destinify.backend.controllers.PagBankWebhookController;
import jakarta.validation.constraints.Null;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PagBankWebhookSignatureTest {

    private static final String TOKEN = "token-de-teste";

    private static final String CORPO = "{\"id\":\"ORDE_1\",\"status\":\"PAID\"}";
    private static final String HASH_CORPO = "7addef7cfefba9aa99b9e94c52572e9c34827697990760d0fa9eed04b98b01a3";

    // Corpo com acento: prova que o hash é calculado sobre os BYTES em UTF-8
    private static final String CORPO_ACENTO = "{\"city\":\"São Paulo\",\"status\":\"PAID\"}";
    private static final String HASH_ACENTO = "89b05c17d8d6bb8d713d93dcf234bc9303b8c4bf9c2c957fd9cc48ad181c673b";

    // O mesmo conteúdo, mas com espaços (JSON "formatado"): o hash muda. A documentação avisa disso.
    private static final String CORPO_FORMATADO = "{ \"id\": \"ORDE_1\", \"status\": \"PAID\" }";
    private static final String HASH_FORMATADO = "bfb54478d248eec1cfda65149e381d4fa5fbd4bde2a2d2311212aa08dfdc2c10";

    private PagBankWebhookSignature signature;

    @BeforeEach
    void setUp() {
        signature = new PagBankWebhookSignature(TOKEN);
    }

    private static byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void calcular_bateComOValorDeReferencia() {
        assertThat(signature.calcular(bytes(CORPO))).isEqualTo(HASH_CORPO);
    }

    @Test
    void calcular_comAcento_usaBytesUtf8() {
        assertThat(signature.calcular(bytes(CORPO_ACENTO))).isEqualTo(HASH_ACENTO);
    }

    @Test
    void calcular_coporFormatado_geraOutroHash() {
        assertThat(signature.calcular(bytes(CORPO_FORMATADO))).isEqualTo(HASH_FORMATADO);
        assertThat(signature.calcular(bytes(HASH_FORMATADO))).isNotEqualTo(HASH_CORPO);
    }

    @Test
    void valida_assinaturaCorreta_retornaTrue() {
        assertThat(signature.valida(bytes(CORPO), HASH_CORPO)).isTrue();
    }

    @Test
    void valida_toleraMaiusculasEEspacosNoCabecalho(){
        assertThat(signature.valida(bytes(CORPO), "  " + HASH_CORPO.toUpperCase() + " ")).isTrue();
    }

    @Test
    void valida_corpoAlterado_retornaFalse() {
        // Atacante troca PAID por PAYD mantendo a assinatura original
        String adulterado = "{\"id\":\"ORDE1\",\"status\":\"PAYD\"}";

        assertThat(signature.valida(bytes(adulterado), HASH_CORPO)).isFalse();
    }

    @Test
    void valida_assinaturaDeOutroToken_retornaFalse() {
        PagBankWebhookSignature outroToken = new PagBankWebhookSignature("token-do-atacante");

        assertThat(signature.valida(bytes(CORPO), outroToken.calcular(bytes(CORPO)))).isFalse();
    }

    @ParameterizedTest(name = "cabeçalho [{0}] é recusado")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  ", "nao-e-um-hash", "7addef"})
    void valida_cabecalhoAusenteOuInvalido_retornaFalse(String cabecalho) {
        assertThat(signature.valida(bytes(CORPO), cabecalho)).isFalse();
    }

    @Test
    void valida_corpoNulo_retornaFalse() {
        assertThat(signature.valida(null, HASH_CORPO)).isFalse();
    }

    @ParameterizedTest(name = "token [{0}] nunca valida")
    @NullAndEmptySource
    void valida_tokenVazio_nuncaValida(String tokenVazio) {
        PagBankWebhookSignature semToken = new PagBankWebhookSignature(tokenVazio);
        // O atacante calcula o hash de "-" + corpo (token vazio) e o envia como assinatura.
        String hashSemToken = semToken.calcular(bytes(CORPO));

        assertThat(semToken.valida(bytes(CORPO), hashSemToken)).isFalse();
    }
}
