package com.maisprati.destinify.backend.servicies;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.maisprati.destinify.backend.domain.User;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;


@Service
public class TokenService {

    public String generatToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256("123456789");
            return JWT.create()
                    .withIssuer("Destinify")
                    .withSubject(user.getUsername())
                    .withExpiresAt(expiration(30))
                    .sign(algorithm);
        }catch (JWTCreationException creationException) {
           // throw new bisnessRule("Erro ao gerar token JWT");
            throw new RuntimeException("Erro ao gerar token jwt", creationException);
        }
    }

    public String verifyToken(String token) {
        DecodedJWT decodedJWT;
        try {
            Algorithm algorithm = Algorithm.HMAC256("123456789");
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
            Algorithm algorithm = Algorithm.HMAC256("123456789");
            return JWT.create()
                    .withIssuer("Destinify")
                    .withSubject(user.getId().toString())
                    .withExpiresAt(expiration(120))
                    .sign(algorithm);
        }catch (JWTCreationException creationException) {
            // throw new bisnessRule("Erro ao gerar token JWT");
            throw new RuntimeException("Erro ao gerar token jwt", creationException);
        }
    }

    private Instant expiration(Integer minutes){
        return Instant.now().plus(Duration.ofMinutes(minutes));
    }
}
