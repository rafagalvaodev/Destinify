package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.TokenService;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.support.WebTestConfig;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    private final User user = TestDataFactory.client(1L);

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("credenciais corretas devolvem accessToken e refreshToken")
        void credenciaisCorretas_retorna200ComTokens() throws Exception {
            Authentication autenticado = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(autenticado);
            when(tokenService.generatToken(user)).thenReturn("access-123");
            when(tokenService.generatRefreshToken(user)).thenReturn("refresh-456");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"maria1@email.com", "password":"senha12345"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access-123"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-456"));

            // o controller montou o pedido de autenticação com o e-mail e a senha recebidos
            ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
            verify(authenticationManager).authenticate(captor.capture());
            assertThat(captor.getValue().getPrincipal()).isEqualTo("maria1@email.com");
            assertThat(captor.getValue().getCredentials()).isEqualTo("senha12345");
        }

        @Test
        @DisplayName("credenciais erradas devolvem 403 com a mensagem 'Email or Password are wrong'")
        void credenciaisErradas_retorna403() throws Exception {
            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"maria1@email.com", "password":"errada"}
                            """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Email or password are wrong"));

            verify(tokenService, never()).generatToken(any(User.class));    // nhum token foi emitido
        }

        @Test
        @DisplayName("e-mail ou senha em branco devolvem 400 e nem tentam autenticar")
        void camposEmBranco_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"email":"", "password":" "}
                            """))
                    .andExpect(status().isBadRequest());

            verify(authenticationManager, never()).authenticate(any(Authentication.class));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/update-token")
    class AtualizarToken {

        @Test
        @DisplayName("refresh token válido gera um novo par de tokens")
        void refreshValido_retorna200() throws Exception {
            // No refresh token o "subject" é o ID do usuário (veja TokenService.generatRefreshToken)
            when(tokenService.verifyToken("refresh-antigo")).thenReturn("1");
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(tokenService.generatToken(user)).thenReturn("novo-access");
            when(tokenService.generatRefreshToken(user)).thenReturn("novo-refresh");

            mockMvc.perform(post("/api/auth/update-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"refreshToken":"refresh-antigo"}
                            """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("novo-access"))
                    .andExpect(jsonPath("$.refreshToken").value("novo-refresh"));
        }

        @Test
        @DisplayName("refresh token em branco devolve 400")
        void refreshEmBranco_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/update-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"refreshToken":""}
                            """))
                    .andExpect(status().isBadRequest());

            verify(tokenService, never()).verifyToken(any());
        }
    }
}
