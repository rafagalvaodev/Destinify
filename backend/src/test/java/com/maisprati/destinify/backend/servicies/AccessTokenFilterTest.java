package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.support.TestDataFactory;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccessTokenFilter")
class AccessTokenFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private AccessTokenFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        request = new MockHttpServletRequest("GET", "/api/users/me");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("sem cabeçalho Authorization: segue anônimo e nem tenta validar token")
    void semCabecalho_naoAutentica() throws Exception {
        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenService, never()).verifyToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("cabeçalho que não é Bearer (ex.: Basic) é ignorado")
    void cabecalhoNaoBearer_ignora() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenService, never()).verifyToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // NOVO
    @Test
    @DisplayName("'Bearer ' sem nenhum token é ignorado")
    void bearerVazio_ignora() throws Exception {
        request.addHeader("Authorization", "Bearer ");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenService, never()).verifyToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("token válido: autentica o usuário com as autoridades dele")
    void tokenValido_autenticaUsuario() throws Exception {
        User user = TestDataFactory.admin(2L);
        request.addHeader("Authorization", "Bearer token-valido");
        when(tokenService.verifyToken("token-valido")).thenReturn("admin2@email.com");
        when(userRepository.findByEmailIgnoreCase("admin2@email.com")).thenReturn(Optional.of(user));

        filter.doFilter(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isSameAs(user); // é isso que o @AuthenticationPrincipal recebe
        assertThat(auth.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("token inválido: não autentica, mas deixa a requisição seguir")
    void tokenInvalido_naoAutenticaMasSegue() throws Exception {
        request.addHeader("Authorization", "Bearer token-podre");
        when(tokenService.verifyToken("token-podre")).thenThrow(new RuntimeException("Erro ao verificar token jwt"));

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response); // o filtro não derruba a requisição
    }

    @Test
    @DisplayName("token válido de usuário que não existe mais: não autentica")
    void usuarioNaoEncontrado_naoAutentica() throws Exception {
        request.addHeader("Authorization", "Bearer token-valido");
        when(tokenService.verifyToken("token-valido")).thenReturn("fantasma@email.com");
        when(userRepository.findByEmailIgnoreCase("fantasma@email.com")).thenReturn(Optional.empty());

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}