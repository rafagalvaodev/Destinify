package com.maisprati.destinify.backend.servicies;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class LoginGoogleService {

    @Value("${google.oauth.client.id}")
    private String clientId;

    @Value("${google.oauth.client.secret}")
    private String clientSecret;

    private final String redirectUri =
            "http://localhost:8080/api/auth/login/google/autorizado";

    private final RestClient restClient;

    public LoginGoogleService(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public String gerarUrl() {
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=openid%20email"
                + "&response_type=code";
    }

    private String obterToken(String code) {
        var dados = new LinkedMultiValueMap<String, String>();
        dados.add("code", code);
        dados.add("client_id", clientId);
        dados.add("client_secret", clientSecret);
        dados.add("redirect_uri", redirectUri);
        dados.add("grant_type", "authorization_code");

        Map<?, ?> resposta = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(dados)
                .retrieve()
                .body(Map.class);

        if (resposta == null
                || !(resposta.get("access_token") instanceof String accessToken)) {
            throw new IllegalStateException(
                    "Google não retornou um access_token");
        }

        return accessToken;
    }

    public String obterEmail(String code) {
        String accessToken = obterToken(code);

        Map<?, ?> perfil = restClient.get()
                .uri("https://openidconnect.googleapis.com/v1/userinfo")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(Map.class);

        if (perfil == null
                || !Boolean.TRUE.equals(perfil.get("email_verified"))
                || !(perfil.get("email") instanceof String email)
                || email.isBlank()) {
            throw new IllegalStateException(
                    "Google não retornou um e-mail verificado");
        }

        return email;
    }
}