package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.LoginDTO.TokenResponseDTO;
import com.maisprati.destinify.backend.domain.enums.Role;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.GoogleLoginTicketService;
import com.maisprati.destinify.backend.servicies.LoginGoogleService;
import com.maisprati.destinify.backend.servicies.TokenService;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.support.WebTestConfig;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GoogleAuthController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("GoogleAuthController")
class GoogleAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginGoogleService loginGoogleService;

    @MockitoBean
    private GoogleLoginTicketService googleLoginTicketService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserRepository userRepository;

    // O controller grava a autenticação no SecurityContextHolder (global): limpamos depois.
    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/auth/login redireciona (302) para a URL de autorização do Google")
    void redirecionarGoogle_retorna302() throws Exception {
        when(loginGoogleService.gerarUrl()).thenReturn("https://accounts.google.com/o/oauth2/v2/auth?client_id=x");

        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://accounts.google.com/o/oauth2/v2/auth?client_id=x"));
    }

    @Nested
    @DisplayName("GET /api/auth/login/google/autorizado")
    class Autorizado {

        @Test
        @DisplayName("usuário já cadastrado: não cria outro e redireciona ao front com o ticket")
        void usuarioExistente_naoCriaERedireciona() throws Exception {
            User existente = TestDataFactory.client(1L);
            when(loginGoogleService.obterEmail("codigo-abc")).thenReturn("maria1@email.com");
            when(userRepository.findByEmailIgnoreCase("maria1@email.com")).thenReturn(Optional.of(existente));
            when(tokenService.generatToken(existente)).thenReturn("access");
            when(tokenService.generatRefreshToken(existente)).thenReturn("refresh");
            when(googleLoginTicketService.create(new TokenResponseDTO("access", "refresh"))).thenReturn("ticket-1");

            mockMvc.perform(get("/api/auth/login/google/autorizado").param("code", "codigo-abc"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "http://localhost:5173/#googleTicket=ticket-1"));

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("usuário novo: é criado como CLIENT, nome vem do e-mail e senha é aleatória criptografada")
        void usuarioNovo_criaComoClient() throws Exception {
            when(loginGoogleService.obterEmail("codigo-abc")).thenReturn("novo.usuario@gmail.com");
            when(userRepository.findByEmailIgnoreCase("novo.usuario@gmail.com")).thenReturn(Optional.empty());
            // o save devolve o próprio objeto recebido (simula o banco)
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(tokenService.generatToken(any(User.class))).thenReturn("access");
            when(tokenService.generatRefreshToken(any(User.class))).thenReturn("refresh");
            when(googleLoginTicketService.create(any(TokenResponseDTO.class))).thenReturn("ticket-2");

            mockMvc.perform(get("/api/auth/login/google/autorizado").param("code", "codigo-abc"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", "http://localhost:5173/#googleTicket=ticket-2"));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User criado = captor.getValue();
            assertThat(criado.getEmail()).isEqualTo("novo.usuario@gmail.com");
            assertThat(criado.getName()).isEqualTo("novo.usuario");     // texto antes do "@"
            assertThat(criado.getRole()).isEqualTo(Role.CLIENT);
            assertThat(criado.getPassword()).startsWith("$2");                  // hash BCrypt, nunca texto puro
        }

        @Test
        @DisplayName("sem o parâmetro 'code':400")
        void semCode_retorna400() throws Exception {
            mockMvc.perform(get("/api/auth/login/google/autorizado"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login/google/exchange")
    class Exchange {

        @Test
        @DisplayName("ticket válido devolve os token (rota pública)")
        void ticketValido_retorna200() throws Exception {
            when(googleLoginTicketService.read("ticket-1")).thenReturn(new TokenResponseDTO("access", "refresh"));

            mockMvc.perform(post("/api/auth/login/google/exchange").param("ticket", "ticket-1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").value("access"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh"));
        }

        @Test
        @DisplayName("ticket expirado/inválido devolve 400")
        void ticketInvalido_retorna400() throws Exception {
            when(googleLoginTicketService.read("velho"))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Login has expired"));

            mockMvc.perform(post("/api/auth/login/google/exchange").param("ticket", "velho"))
                    .andExpect(status().isBadRequest());
        }
    }
}
