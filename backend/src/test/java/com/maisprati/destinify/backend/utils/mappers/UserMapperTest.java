package com.maisprati.destinify.backend.utils.mappers;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserCreate;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserResponse;
import com.maisprati.destinify.backend.domain.enums.Role;
import com.maisprati.destinify.backend.support.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;


//  Testa a conversão DTO <-> Entidade do usuário.


@DisplayName("UserMapper")
class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserMapper();
    }

    @Test
    @DisplayName("userCreateMapper copia nome, e-mail, senha e data de nascimento")
    void userCreateMapper_copiaOsCampos() {
        UserCreate dto = new UserCreate("Maria", "maria@email.com", LocalDate.of(2000, 3, 15), "senha12345");

        User user = mapper.userCreateMapper(dto);

        assertThat(user.getName()).isEqualTo("Maria");
        assertThat(user.getEmail()).isEqualTo("maria@email.com");
        assertThat(user.getPassword()).isEqualTo("senha12345"); // ainda não criptografada: isso é papel do UserService
        assertThat(user.getBirthdate()).isEqualTo(LocalDate.of(2000, 3, 15));
    }

    @Test
    @DisplayName("userCreateMapper não define id nem role (quem define é o service)")
    void userCreateMapper_naoDefineIdNemRole() {
        UserCreate dto = new UserCreate("Maria", "maria@email.com", LocalDate.of(2000, 3, 15), "senha12345");

        User user = mapper.userCreateMapper(dto);

        assertThat(user.getId()).isNull();
        assertThat(user.getRole()).isNull();
    }

    @Test
    @DisplayName("userCreateMapper devolve null quando recebe null")
    void userCreateMapper_comNull_retornaNull() {
        assertThat(mapper.userCreateMapper(null)).isNull();
    }

    @Test
    @DisplayName("userResponseMapper converte a entidade e não expõe a senha")
    void userResponseMapper_copiaCamposPublicos() {
        User user = TestDataFactory.admin(7L);

        UserResponse response = mapper.userResponseMapper(user);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Admin Silva");
        assertThat(response.email()).isEqualTo("admin7@email.com");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1985, 1, 10));
        assertThat(response.role()).isEqualTo(Role.ADMIN);
        // O record UserResponse nem possui o campo "password": por isso é impossível vazar a senha.
    }
}