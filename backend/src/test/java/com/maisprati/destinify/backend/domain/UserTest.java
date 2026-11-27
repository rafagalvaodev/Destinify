package com.maisprati.destinify.backend.domain;

import com.maisprati.destinify.backend.domain.enums.Role;
import com.maisprati.destinify.backend.support.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a entidade User, que também é um UserDetails (contrato do Spring Security).
 * Entidade tem pouca lógica, mas aqui há regras importantes para a segurança:
 * - o "username" do login é o e-mail;
 * - a autoridade é "ROLE_" + papel (é o prefixo que o hasRole("ADMIN") do SecurityConfig espera!).
 * Se alguém tirar o prefixo "ROLE_", TODAS as rotas de admin passem a negar acesso.
 */

@DisplayName("User (entidade)")
class UserTest {

    @Test
    @DisplayName("o username usado no login é o e-mail")
    void getUsername_retornaOEmail() {
        User user = TestDataFactory.client(1L);

        assertThat(user.getUsername()).isEqualTo("maria1@email.com");
    }

    @Test
    @DisplayName("CLIENT recebe a autoridade ROLE_CLIENT")
    void getAuthorities_clientTemRoleClient() {
        User user = TestDataFactory.client(1L);

        // extracting(...) pega um campo de cada item da coleção para comparar só ele
        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLIENT");
    }

    @Test
    @DisplayName("ADMIN recebe a autoridade ROLE_ADMIN")
    void getAuthorities_adminTemRoleAdmin() {
        User user = TestDataFactory.admin(2L);

        assertThat(user.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("a conta nunca está bloqueada/expirada e está sempre habilitada")
    void flagsDeConta_sempreAtivas() {
        User user = TestDataFactory.client(1L);

        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("dois usuários com o mesmo id são iguais (equals/hashCode olham só o id)")
    void equals_comparaPeloId() {
        User a = new User(1L, "Ana", "ana@email.com", "x", null, Role.CLIENT);
        User b = new User(1L, "Outro Nome", "outro@email.com", "y", null, Role.ADMIN);
        User c = new User(2L, "Ana", "ana@email.com", "x", null, Role.CLIENT);

        assertThat(a).isEqualTo(b);             // mesmo id -> iguais, mesmo com dados diferentes
        assertThat(a).hasSameHashCodeAs(b);     // contrato: iguais => mesmo hashCode
        assertThat(a).isNotEqualTo(c);          // ids diferentes -> diferentes
    }
}