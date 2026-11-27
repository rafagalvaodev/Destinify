package com.maisprati.destinify.backend.servicies;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("LoginGoogleService")
class LoginGoogleServiceTest {

     private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
     private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

     private MockRestServiceServer server;
     private LoginGoogleService service;

     @BeforeEach
    void setUp() {
         RestClient.Builder builder = RestClient.builder();
         server = MockRestServiceServer.bindTo(builder).build();

         service = new LoginGoogleService(builder);
         ReflectionTestUtils.setField(service, "clientId", "meu-client-id");
         ReflectionTestUtils.setField(service, "clientSecret", "meu-segredo");
     }

    @Test
    @DisplayName("geraUrl monta a URL de autorização do Google com client_id, redirect e escopo")
    void gerarUrl_montaUrlDeAutorizacao() {
         String url = service.gerarUrl();

         assertThat(url).isEqualTo("https://accounts.google.com/o/oauth2/v2/auth"
                 + "?client_id=meu-client-id"
                 + "&redirect_uri=http://localhost:8080/api/auth/login/google/autorizado"
                 + "&scope=openid%20email"
                 + "&response_type=code");
    }

    @Test
    @DisplayName("obterEmail troca o code por token e devolve o e-mail verificado")
    void obterEmail_fluxoFeliz_retornaEmail() {
         // 1ª chamada esperada: POST no endpoint de token, com o formulário correto
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("code=codigo-abc")))
                .andExpect(content().string(containsString("client_id=meu-client-id")))
                .andExpect(content().string(containsString("grant_type=authorization_code")))
                .andRespond(withSuccess("{\"access_token\":\"token-123\"}", MediaType.APPLICATION_JSON));

        // 2ª chamada esperada: GET no userinfo usando o token recebido como Bearer
        server.expect(requestTo(USERINFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-123"))
                .andRespond(withSuccess(
                        "{\"email\":\"usuario@gmail.com\",\"email_verified\":true}", MediaType.APPLICATION_JSON));

        String email = service.obterEmail("codigo-abc");

        assertThat(email).isEqualTo("usuario@gmail.com");
        server.verify(); // confirma que todas as chamadas esperadas aconteceram
    }

    @Test
    @DisplayName("obterEmail falha quando o Google não devolve access_token")
    void obterEmail_semAccessToken_lancaExcecao() {
         server.expect(requestTo(TOKEN_URL))
                 .andRespond(withSuccess("{\"erro\":\"invalid_grant\"}", MediaType.APPLICATION_JSON));

         assertThatThrownBy(() -> service.obterEmail("codigo-ruim"))
                 .isInstanceOf(IllegalStateException.class)
                 .hasMessage("Google não retornou um access_token");
    }

    @ParameterizedTest(name = "perfil inválido: {0}")
    @ValueSource(strings = {
            "{\"email\":\"a@b.com\",\"email_verified\":false}", // e-mail não verificado
            "{\"email\":\"a@b.com\"}",                          // sem o campo email_verified
            "{\"email_verified\":true}",                        // sem e-mail                        // sem e-mail
            "{\"email\":\"   \",\"email_verified\":true}"       // e-mail em branco
    })
    @DisplayName("obterEmail rejeita perfis sem e-mail verificado")
    void obterEmail_perfilInvalido_lancaExcecao(String perfilJson) {
         server.expect(requestTo(TOKEN_URL))
                 .andRespond(withSuccess("{\"access_token\":\"token-123\"}", MediaType.APPLICATION_JSON));
         server.expect(requestTo(USERINFO_URL))
                 .andRespond(withSuccess(perfilJson, MediaType.APPLICATION_JSON));

         assertThatThrownBy(() -> service.obterEmail("codigo-abc"))
                 .isInstanceOf(IllegalStateException.class)
                 .hasMessage("Google não retornou um e-mail verificado");
    }
}
