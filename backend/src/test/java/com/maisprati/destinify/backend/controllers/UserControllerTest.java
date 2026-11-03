package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UpdatePassword;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UpdateUser;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserCreate;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserResponse;
import com.maisprati.destinify.backend.domain.enums.Role;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.TokenService;
import com.maisprati.destinify.backend.servicies.UserService;
import com.maisprati.destinify.backend.support.TestAuth;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.support.WebTestConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("UserController")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;      // usado pelo AccessTokenFilter

    @MockitoBean
    private UserRepository userRepository;  // usado pelo AccessTokenFilter

    private final User cliente = TestDataFactory.client(1L);

    private UserResponse userResponse() {
        return new UserResponse(1L, "Maria Cliente", "maria1@email.com", LocalDate.of(1995, 5, 20), Role.CLIENT);
    }

    @Nested
    @DisplayName("POST /api/users/register (público)")
    class Registro {

        @Test
        @DisplayName("cadastra sem login e devolve 201")
        void validoSemLogin_retorna201() throws Exception {
            when(userService.addUser(any(UserCreate.class))).thenReturn(userResponse());

            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"Maria", "email":"maria@email.com", "birthdate":"2000-01-01", "password":"senha12345"}         
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("maria1@email.com"))
                    .andExpect(jsonPath("$.role").value("CLIENT"))
                    .andExpect(jsonPath("$.password").doesNotExist()); // a resposta nunca traz senha

            verify(userService).addUser(new UserCreate("Maria", "maria@email.com",
                    LocalDate.of(2000, 1, 1), "senha12345"));
        }

        @Test
        @DisplayName("senha curta devolve 400 e nada é cadastrado")
        void senhaCurta_retorna400() throws Exception {
            mockMvc.perform(post("/api/users/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"Maria", "email":"maria@email.com", "birthdate":"2000-01-01", "password":"123"}
                            """))
                    .andExpect(status().isBadRequest());

            verify(userService, never()).addUser(any(UserCreate.class));
        }
    }

    @Nested
    @DisplayName("GET /api/users/me")
    class MeuPerfil {

        @Test
        @DisplayName("logado: devolve o perfil do próprio usuário")
        void logado_retorna200() throws Exception {
            when(userService.findById(1L)).thenReturn(userResponse());

            mockMvc.perform(get("/api/users/me").with(TestAuth.as(cliente)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Maria Cliente"));
        }

        @Test
        @DisplayName("sem login: 403")
        void semLogin_retorna403() throws Exception {
            mockMvc.perform(get("/api/users/me"))
                    .andExpect(status().isForbidden());

            verify(userService, never()).findById(anyLong());
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me")
    class AtualizarPerfil {

        @Test
        @DisplayName("atualiza o perfil do usuário logado (id vem do login, não da URL)")
        void logado_retorna200() throws Exception {
            when(userService.updateProfile(anyLong(), any(UpdateUser.class))).thenReturn(userResponse());

            mockMvc.perform(patch("/api/users/me")
                    .with(TestAuth.as(cliente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"Novo Nome", "email":"novo@email.com", "birthDate":"1990-05-10"}
                            """))
                .andExpect(status().isOk());

            verify(userService).updateProfile(1L, new UpdateUser("Novo Nome",
                    "novo@email.com", null, LocalDate.of(1990, 5, 10)));
        }

        @Test
        @DisplayName("sem login: 403")
        void semLogin_retorna403() throws Exception {
            mockMvc.perform(patch("/api/users/me")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"X\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/me/password")
    class TrocarSenha {

        @Test
        @DisplayName("troca a senha devolve 204 sem corpo")
        void valido_retorna204() throws Exception {
            mockMvc.perform(patch("/api/users/me/password")
                    .with(TestAuth.as(cliente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"currentPassword":"antiga", "newPassword":"novaSenha1", "confirmNewPassword":"novaSenha1"}
                            """))
                .andExpect(status().isNoContent());

            verify(userService).updatePassword(1L, new UpdatePassword("antiga", "novaSenha1", "novaSenha1"));
        }

        @Test
        @DisplayName("campo em branco devolve 400")
        void campoEmBranco_retorna400() throws Exception {
            mockMvc.perform(patch("/api/users/me/password")
                    .with(TestAuth.as(cliente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"currentPassword":"", "newPassword":"novaSenha1", "confirmNewPassword":"novaSenha1"}
                            """))
                .andExpect(status().isBadRequest());

            verify(userService, never()).updatePassword(anyLong(), any(UpdatePassword.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/me")
    class ExcluirConta {

        @Test
        @DisplayName("exclui a conta do usuário logado e devolve 204")
        void logado_retorna204() throws Exception {
            mockMvc.perform(delete("/api/users/me")
                    .with(TestAuth.as(cliente)))
                .andExpect(status().isNoContent());

            verify(userService).deleteUserById(1L);
        }

        @Test
        @DisplayName("sem login: 403 e nada é excluído")
        void semLogin_retorna403() throws Exception {
            mockMvc.perform(delete("/api/users/me"))
                    .andExpect(status().isForbidden());

            verify(userService, never()).deleteUserById(anyLong());
        }
    }

    @Nested
    @DisplayName("GET /api/users/me/name")
    class MeuNome {

        @Test
        @DisplayName("devolve só o nome do usuário logado")
        void logado_retornaNome() throws Exception {
            when(userService.findUserById(1L)).thenReturn("maria cliente");

            mockMvc.perform(get("/api/users/me/name").with(TestAuth.as(cliente)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("maria cliente"));
        }
    }

    @Nested
    @DisplayName("GET /api/users/all-users (somente ADMIN)")
    class ListarUsuarios {

        @Test
        @DisplayName("ADMIN recebe a lista")
        void admin_retorna200() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of(userResponse()));

            mockMvc.perform(get("/api/users/all-users").with(TestAuth.as(TestDataFactory.admin(9L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].email").value("maria1@email.com"));
        }

        @Test
        @DisplayName("CLIENT recebe 403")
        void client_retorna403() throws Exception {
            mockMvc.perform(get("/api/users/all-users").with(TestAuth.as(cliente)))
                    .andExpect(status().isForbidden());

            verify(userService, never()).getAllUsers();
        }
    }

    @Nested
    @DisplayName("autenticação por token bearer (AccessTokenFilter real)")
    class AutenticacaoPorToken {

        @Test
        @DisplayName("cabeçalho 'Authorization: Bearer <token>' válido autentica o usuário")
        void bearerValido_autentica() throws Exception {
            // O filtro vai: ler o token -> pedir o e-mail ao TokenService -> buscar o User no repositório
            when(tokenService.verifyToken("token-valido")).thenReturn("maria1@email.com");
            when(userRepository.findByEmailIgnoreCase("maria1@email.com")).thenReturn(Optional.of(cliente));
            when(userService.findById(1L)).thenReturn(userResponse());

            mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer token-valido"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("maria1@email.com"));
        }

        @Test
        @DisplayName("token inválido não autentica: a rota protegida devolve 403")
        void bearerInvalido_retorna403() throws Exception {
            when(tokenService.verifyToken("token-podre")).thenThrow(new RuntimeException("Erro ao verificar token jwt"));

            mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer token-podre"))
                    .andExpect(status().isForbidden());

            verify(userService, never()).findById(anyLong());
        }
    }
}
