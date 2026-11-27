package com.maisprati.destinify.backend.servicies;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.maisprati.destinify.backend.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;


@Service
public class TokenService {

    // Segredo que assina os tokens JWT. Em produção vem da variável JWT_SECRET.
    // O valor depois do ":" só vale se nada for configurado (dev e testes).
    @Value("${destinify.jwt.secret:123456789}")
    private String secret = "123456789";

    public String generatToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("Destinify")
                    .withSubject(user.getUsername())
                    .withExpiresAt(expiration(30))
                    .sign(algorithm);
        }catch (JWTCreationException creationException) {
            throw new RuntimeException("Erro ao gerar token jwt", creationException);
        }
    }

    public String verifyToken(String token) {
        DecodedJWT decodedJWT;
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            JWTVerifier verifier = JWT
                    .require(algorithm)
                    .withIssuer("Destinify")
                    .build();

            decodedJWT = verifier.verify(token);
            return decodedJWT.getSubject();
        }catch (JWTVerificationException jwtVerificationException) {
            throw new RuntimeException("Erro ao verificar token jwt", jwtVerificationException);
        }
    }

    public String generatRefreshToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("Destinify")
                    .withSubject(user.getId().toString())
                    .withExpiresAt(expiration(120))
                    .sign(algorithm);
        }catch (JWTCreationException creationException) {
            throw new RuntimeException("Erro ao gerar token jwt", creationException);
        }
    }

    private Instant expiration(Integer minutes){
        return Instant.now().plus(Duration.ofMinutes(minutes));
    }
}
