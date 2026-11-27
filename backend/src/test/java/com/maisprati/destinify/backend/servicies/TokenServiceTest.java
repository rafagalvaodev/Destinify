package com.maisprati.destinify.backend.servicies;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.support.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TokenService (JWT)")
class TokenServiceTest {

    private static final String SEGREDO = "123456789";
    private static final String EMISSOR = "Destinify";

    private TokenService tokenService;
    private User user;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService();
        user = TestDataFactory.client(5L);
    }

    @Test
    @DisplayName("o access token tem o formato de JWT (3 partes separadas por ponto)")
    void generatToken_temFormatoJwt() {
        String token = tokenService.generatToken(user);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3); // header.payload.assinatura
    }

    // NOVO
    @Test
    @DisplayName("o access token carrega o e-mail do usuário como 'subject'")
    void generatToken_subjectEhOEmail() {
        String token = tokenService.generatToken(user);

        assertThat(tokenService.verifyToken(token)).isEqualTo("maria5@email.com");
    }

    @Test
    @DisplayName("o access token expira em aproximadamente 30 minutos")
    void generatToken_expiraEm30Minutos() {
        Instant expiracao = JWT.decode(tokenService.generatToken(user)).getExpiresAt().toInstant();

        // margem de 1 minuto para cima/baixo, porque o relógio anda entre a geração e a conferência
        assertThat(expiracao).isBetween(
                Instant.now().plusSeconds(29 * 60),
                Instant.now().plusSeconds(31 * 60));
    }

    @Test
    @DisplayName("o refresh token carrega o ID do usuário como 'subject'")
    void generatRefreshToken_subjectEhOId() {
        String refresh = tokenService.generatRefreshToken(user);

        assertThat(tokenService.verifyToken(refresh)).isEqualTo("5");
    }

    @Test
    @DisplayName("o refresh token expira em aproximadamente 120 minutos")
    void generatRefreshToken_expiraEm120Minutos() {
        Instant expiracao = JWT.decode(tokenService.generatRefreshToken(user)).getExpiresAt().toInstant();

        assertThat(expiracao).isBetween(
                Instant.now().plusSeconds(119 * 60),
                Instant.now().plusSeconds(121 * 60));
    }

    @Test
    @DisplayName("verifyToken rejeita um texto que não é JWT")
    void verifyToken_lixo_lancaExcecao() {
        assertThatThrownBy(() -> tokenService.verifyToken("isso-nao-e-um-jwt"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao verificar token jwt")
                .hasCauseInstanceOf(JWTVerificationException.class);
    }

    @Test
    @DisplayName("verifyToken rejeita token assinado com outro segredo")
    void verifyToken_assinaturaInvalida_lancaExcecao() {
        String forjado = JWT.create()
                .withIssuer(EMISSOR)
                .withSubject("hacker@email.com")
                .withExpiresAt(Instant.now().plusSeconds(600))
                .sign(Algorithm.HMAC256("outro-segredo")); // segredo diferente

        assertThatThrownBy(() -> tokenService.verifyToken(forjado))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao verificar token jwt");
    }

    // NOVO
    @Test
    @DisplayName("verifyToken rejeita token de outro emissor (issuer)")
    void verifyToken_emissorErrado_lancaExcecao() {
        String deOutroEmissor = JWT.create()
                .withIssuer("OutroSistema")
                .withSubject("maria@email.com")
                .withExpiresAt(Instant.now().plusSeconds(600))
                .sign(Algorithm.HMAC256(SEGREDO)); // segredo certo, emissor errado

        assertThatThrownBy(() -> tokenService.verifyToken(deOutroEmissor))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao verificar token jwt");
    }

    @Test
    @DisplayName("verifyToken rejeita token expirado")
    void verifyToken_expirado_lancaExcecao() {
        String expirado = JWT.create()
                .withIssuer(EMISSOR)
                .withSubject("maria1@email.com")
                .withExpiresAt(Instant.now().minusSeconds(3600)) // venceu há 1 hora
                .sign(Algorithm.HMAC256(SEGREDO));

        assertThatThrownBy(() -> tokenService.verifyToken(expirado))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Erro ao verificar token jwt");
    }
}