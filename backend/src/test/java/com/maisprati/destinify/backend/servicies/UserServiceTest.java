package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UpdatePassword;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UpdateUser;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserCreate;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserResponse;
import com.maisprati.destinify.backend.domain.enums.Role;
import com.maisprati.destinify.backend.exceptions.UserNotFoundException;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.utils.mappers.UserMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    // loadUserByUsername (usado pelo Spring Security no login)

    @Test
    @DisplayName("loadUserByUsername devolve o usuário encontrado pelo e-mail")
    void loadUserByUsername_usuarioExiste_retornaUsuario() {
        User user = TestDataFactory.client(1L);
        when(userRepository.findByEmailIgnoreCase("maria1@email.com")).thenReturn(Optional.of(user));

        var result = userService.loadUserByUsername("maria1@email.com");

        assertThat(result).isSameAs(user);
    }

    @Test
    @DisplayName("loadUserByUsername lança exceção quando o e-mail não existe")
    void loadUserByUsername_usuarioNaoExiste_lancaExcecao() {

        assertThatThrownBy(() -> userService.loadUserByUsername("ninguem@email.com"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // addUser (cadastro)

    @Test
    @DisplayName("addUser criptografa a senha, força role CLIENT e salva")
    void addUser_criptografaSenhaEDefineRoleClient() {
        // Arrange
        UserCreate dto = new UserCreate("Maria", "maria@email.com", LocalDate.of(2000, 1, 1), "senha12345");

        // O mapper devolve um usuário com a senha EM TEXTO PURO e, de propósito, role ADMIN,
        // para provar que o service não confia nisso e força CLIENT (evita escalada de privilégio).
        User mapped = new User();
        mapped.setName("Maria");
        mapped.setEmail("maria@email.com");
        mapped.setPassword("senha12345");
        mapped.setRole(Role.ADMIN);

        User saved = TestDataFactory.client(1L);
        UserResponse response = new UserResponse(1L, "Maria", "maria@email.com", LocalDate.of(2000, 1, 1), Role.CLIENT);

        when(userMapper.userCreateMapper(dto)).thenReturn(mapped);
        when(passwordEncoder.encode("senha12345")).thenReturn("HASH_BCRYPT");
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.userResponseMapper(saved)).thenReturn(response);

        // Act
        UserResponse result = userService.addUser(dto);

        // Assert 1: o retorno é o que o mapper devolveu
        assertThat(result).isEqualTo(response);

        // Assert 2: o que foi enviado ao banco? Capturamos o argumento do save(...).
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();

        assertThat(persisted.getPassword()).isEqualTo("HASH_BCRYPT"); // nunca a senha pura!
        assertThat(persisted.getRole()).isEqualTo(Role.CLIENT);
    }

    // deleteUserByID

    @Test
    @DisplayName("deleteUserById remove o usuário quando ele existe")
    void deleteUserById_existe_remove() {
        User user = TestDataFactory.client(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUserById(1L);

        verify(userRepository).delete(user);
    }

    @Test
    @DisplayName("deleteUserById não faz nada (e não dá erro) quando o usuário não existe")
    void deleteUserById_naoExiste_naoRemoveNada() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        userService.deleteUserById(99L); // não lança exceção: é idempotente

        verify(userRepository, never()).delete(any(User.class));
    }

    // getAllUsers

    @Test
    @DisplayName("getAllUsers converte todos os usuários em respostas")
    void getAllUsers_converteTodos() {
        User u1 = TestDataFactory.client(1L);
        User u2 = TestDataFactory.client(2L);
        UserResponse r1 = new UserResponse(1L, "Maria Cliente", "maria1@email.com", null, Role.CLIENT);
        UserResponse r2 = new UserResponse(2L, "Admin Silva", "admin2@email.com", null, Role.ADMIN);

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        // PotentialStubbingProblem.
        doReturn(r1).when(userMapper).userResponseMapper(u1);
        doReturn(r2).when(userMapper).userResponseMapper(u2);

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).containsExactly(r1, r2); // mesmos itens, na mesma ordem
    }

    @Test
    @DisplayName("getAllUsers devolve lista vazia quando não há usuários")
    void getAllUsers_semUsuarios_retornaListaVazia() {
        when(userRepository.findAll()).thenReturn(List.of());

        assertThat(userService.getAllUsers()).isEmpty();
    }

    // updateProfile

    @Test
    @DisplayName("updateProfile atualiza nome, e-mail e data de nascimento")
    void updateProfile_atualizaCampos() {
        User user = TestDataFactory.client(1L);
        UpdateUser dto = new UpdateUser("Novo Nome", "novo@email.com", null, LocalDate.of(1990, 12, 25));
        UserResponse response = new UserResponse(1L, "Novo Nome", "novo@email.com", LocalDate.of(1990, 12, 25), Role.CLIENT);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.userResponseMapper(user)).thenReturn(response);

        UserResponse result = userService.updateProfile(1L, dto);

        assertThat(result).isEqualTo(response);
        // O objeto "user" foi alterado pelo service (é a mesma instância que o repository devolveu)
        assertThat(user.getName()).isEqualTo("Novo Nome");
        assertThat(user.getEmail()).isEqualTo("novo@email.com");
        assertThat(user.getBirthdate()).isEqualTo(LocalDate.of(1990, 12, 25));
    }

    @Test
    @DisplayName("updateProfile lança UserNotFoundException quando o usuário não existe")
    void updateProfile_naoExiste_lancaExcecao() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        UpdateUser dto = new UpdateUser("Nome", "e@email.com", null, null);

        assertThatThrownBy(() -> userService.updateProfile(99L, dto))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 99 not found");

        verify(userRepository, never()).save(any(User.class)); // nada pode ser salvo
    }

    // updatePassword

    @Test
    @DisplayName("updatePassword troca a senha quando a atual está correta e a confirmação confere")
    void updatePassword_dadosValidos_trocaASenha() {
        User user = TestDataFactory.client(1L);
        user.setPassword("HASH_ANTIGO");
        UpdatePassword dto = new UpdatePassword("senhaAtual", "novaSenha123", "novaSenha123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senhaAtual", "HASH_ANTIGO")).thenReturn(true);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("HASH_NOVO");

        userService.updatePassword(1L, dto);

        assertThat(user.getPassword()).isEqualTo("HASH_NOVO");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("updatePassword falha quando a senha atual está errada")
    void updatePassword_senhaAtualErrada_falha() {
        User user = TestDataFactory.client(1L);
        user.setPassword("HASH_ANTIGO");
        UpdatePassword dto = new UpdatePassword("errada", "novaSenha123", "novaSenha123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada", "HASH_ANTIGO")).thenReturn(false);

        assertThatThrownBy(() -> userService.updatePassword(1L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Current password incorrect");

        // Segurança: se a senha atual está errada, não pode haver criptografia nova sem save
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updatePassword falha quando a nova senha e a confirmação são diferentes")
    void updatePassword_confirmacaoDiferente_falha() {
        User user = TestDataFactory.client(1L);
        user.setPassword("HASH_ANTIGO");
        UpdatePassword dto = new UpdatePassword("senhaAtual", "novaSenha123", "outraCoisa999");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("senhaAtual", "HASH_ANTIGO")).thenReturn(true);

        assertThatThrownBy(() -> userService.updatePassword(1L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("The new password and the confirmNewPassword are not equals");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("updatePassword lança UserNotFoundException quando o usuário não existe")
    void updatePassword_usuarioNaoExiste_lancaExcecao() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        UpdatePassword dto = new UpdatePassword("a", "b", "b");

        assertThatThrownBy(() -> userService.updatePassword(99L, dto))
                .isInstanceOf(UserNotFoundException.class);
    }

    // findById / findUserById

    @Test
    @DisplayName("findById devolve a resposta do usuário")
    void findById_existe_retornaResposta() {
        User user = TestDataFactory.client(1L);
        UserResponse response = new UserResponse(1L, "Maria Cliente", "maria1@email.com", null, Role.CLIENT);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.userResponseMapper(user)).thenReturn(response);

        assertThat(userService.findById(1L)).isEqualTo(response);
    }

    @Test
    @DisplayName("findById lança UserNotFoundException quando não existe")
    void findById_naoExiste_lancaExcecao() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(5L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 5 not found");
    }

    @Test
    @DisplayName("findUserById devolve só o nome, em minúsculas")
    void findUserById_retornaNomeEmMinusculas() {
        User user = TestDataFactory.client(1L);
        user.setName("MARIA SOUZA");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThat(userService.findUserById(1L)).isEqualTo("maria souza");
    }

    @Test
    @DisplayName("findUserById lança UserNotFoundException quando não existe")
    void findUserById_naoExiste_lancaExcecao() {
        when(userRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserById(5L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
