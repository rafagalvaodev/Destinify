package com.maisprati.destinify.backend.domain.dto;

import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingCreate;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelUpdate;
import com.maisprati.destinify.backend.domain.dto.LoginDTO.LoginData;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomCreate;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UpdatePassword;
import com.maisprati.destinify.backend.domain.dto.UserDTO.UserCreate;
import com.maisprati.destinify.backend.domain.enums.RoomType;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Validação dos DTOs")
class DtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void abrirValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        factory.close();
    }

    // Nome dos campos que violaram regras. Ex.: {"name", "stars"}
    private static <T> Set<String> camposInvalidos(T dto) {
        return validator.validate(dto).stream()
                .map(v -> v.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    // Mensagens das violações (úteis quando a mensagem foi personalizada no DTO).
    private static <T> List<String> mensagens(T dto) {
        return validator.validate(dto).stream().map(ConstraintViolation::getMessage).toList();
    }

    @Nested
    @DisplayName("BookingCreate")
    class BookingCreateTests {

        private final LocalDate checkIn = LocalDate.now().plusDays(5);
        private final LocalDate checkOut = LocalDate.now().plusDays(8);

        @Test
        @DisplayName("é válido com todos os campos corretos")
        void valido() {
            assertThat(validator.validate(new BookingCreate(1L, List.of(10L), checkIn, checkOut, 2))).isEmpty();
        }

        @Test
        @DisplayName("hotelId é obrigatório")
        void hotelIdNulo() {
            assertThat(mensagens(new BookingCreate(null, List.of(10L), checkIn, checkOut, 2)))
                    .containsExactly("O id o hotel deve ser preenchido");
        }

        @Test
        @DisplayName("precisa de pelo menos um quarto")
        void semQuartos() {
            assertThat(mensagens(new BookingCreate(1L, List.of(), checkIn, checkOut, 2)))
                    .containsExactly("Pelo menos um quarto deve ser selecionado");
        }

        @Test
        @DisplayName("não aceita id de quarto nulo dentro da lista")
        void quartoComIdNulo() {
            List<Long> ids = new java.util.ArrayList<>();
            ids.add(null); // List.of não aceita null, por isso ArrayList

            assertThat(mensagens(new BookingCreate(1L, ids, checkIn, checkOut, 2)))
                    .containsExactly("O id do quarto deve ser preenchido");
        }

        @Test
        @DisplayName("check-in no passado é recusado")
        void checkInNoPassado() {
            assertThat(mensagens(new BookingCreate(1L, List.of(10L), LocalDate.now().minusDays(1), checkOut, 2)))
                    .containsExactly("A data de check-in não pode ser no passado");
        }

        @Test
        @DisplayName("check-in hoje é aceito (@FutureOrPresent)")
        void checkInHojeEhAceito() {
            assertThat(validator.validate(new BookingCreate(1L, List.of(10L), LocalDate.now(), checkOut, 2))).isEmpty();
        }

        @Test
        @DisplayName("check-out hoje é recusado (@Future exige amanhã em diante)")
        void checkOutHojeEhRecusado() {
            assertThat(mensagens(new BookingCreate(1L, List.of(10L), LocalDate.now(), LocalDate.now(), 2)))
                    .containsExactly("A data de check-out deve ser no futuro");
        }

        @Test
        @DisplayName("datas nulas são recusadas")
        void datasNulas() {
            assertThat(mensagens(new BookingCreate(1L, List.of(10L), null, null, 2)))
                    .containsExactlyInAnyOrder(
                            "A data de check-in deve ser preenchida",
                            "A data de check-out deve ser preenchida");
        }

        @Test
        @DisplayName("hóspedes deve ser maior que zero e obrigatório")
        void hospedesInvalidos() {
            assertThat(mensagens(new BookingCreate(1L, List.of(10L), checkIn, checkOut, 0)))
                    .containsExactly("A quantidade de hóspedes deve ser maior que zero");
            assertThat(mensagens(new BookingCreate(1L, List.of(10L), checkIn, checkOut, null)))
                    .containsExactly("A quantidade de hóspedes deve ser preenchida");
        }
    }

    @Nested
    @DisplayName("HotelCreate")
    class HotelCreateTest {

        private HotelCreate hotel(String name, String city, String address, Double price, Float stars) {
            return new HotelCreate(name, city, address, "desc", price, stars, "img");
        }

        @Test
        @DisplayName("é valido com todos os campos corretors")
        void valido() {
            assertThat(validator.validate(hotel("Hotel", "Rio", "Rua A", 200.0, 4.0f))).isEmpty();
        }

        @Test
        @DisplayName("nome, cidade e endereço não podem ser vazios ou em brancos")
        void textosEmBranco() {
            assertThat(camposInvalidos(hotel("", "", null, 200.0, 4.0f)))
                    .containsExactlyInAnyOrder("name", "city", "address");
        }

        @Test
        @DisplayName("preço por noite deve ser positivo")
        void precoInvalido() {
            assertThat(camposInvalidos(hotel("H", "C", "A", 0.0, 4.0f))).containsExactly("pricePerNight");
            assertThat(camposInvalidos(hotel("H", "C", "A", -10.0, 4.0f))).containsExactly("pricePerNight");
            assertThat(camposInvalidos(hotel("H", "C", "A", null, 4.0f))).containsExactly("pricePerNight");
        }

        @Test
        @DisplayName("estrelas devem estar entre 1 e 5")
        void estrelasForaDoIntervalo() {
            assertThat(camposInvalidos(hotel("H", "C", "A", 100.0, 0.5f))).containsExactly("stars"); // < 1
            assertThat(camposInvalidos(hotel("H", "C", "A", 100.0, 5.5f))).containsExactly("stars"); // > 5
            assertThat(camposInvalidos(hotel("H", "C", "A", 100.0, null))).containsExactly("stars");
            // limites são aceitos
            assertThat(validator.validate(hotel("H", "C", "A", 100.0, 1.0f))).isEmpty();
            assertThat(validator.validate(hotel("H", "C", "A", 100.0, 5.0f))).isEmpty();
        }
    }

    @Nested
    @DisplayName("HotelUpdate")
    class HotelUpdateTests {

        @Test
        @DisplayName("é valido com todos os campos corretos")
        void valido() {
            assertThat(validator.validate(new HotelUpdate("H", "C", "A", "D", 3.0f, "i"))).isEmpty();
        }

        @Test
        @DisplayName("exige nome, cidade, endereço e estrelas")
        void camposObrigatorios() {
            assertThat(camposInvalidos(new HotelUpdate(null, null, null, null, null, null)))
                    .containsExactlyInAnyOrder("name", "city", "address", "stars");
        }
    }

    @Nested
    @DisplayName("UserCreate")
    class UserCreateTests {

        @Test
        @DisplayName("é valido com todos os campos corretos")
        void valido() {
            assertThat(validator.validate(new UserCreate("Maria", "m@email.com", LocalDate.of(2000, 1, 1), "senha12345"))).isEmpty();
        }

        @Test
        @DisplayName("senha com menos de 8 caracteres é recusada com a mensagem em português")
        void senhaCurta() {
            assertThat(mensagens(new UserCreate("Maria", "m@email.com", LocalDate.of(2000, 1, 1), "1234567")))
                    .containsExactly("A senha deve ter no mínimo 8 caracteres");
        }

        @Test
        @DisplayName("senha com exatamente 8 caracteres é aceita")
        void senhaNoLimite() {
            assertThat(validator.validate(new UserCreate("Maria", "m@email.com", LocalDate.of(2000, 1, 1), "12345678"))).isEmpty();
        }

        @Test
        @DisplayName("nome, e-mail e data de nascimento são obrigatórios")
        void camposObrigatorios() {
            assertThat(camposInvalidos(new UserCreate(" ", "", null, "senha12345")))
                    .containsExactlyInAnyOrder("name", "email", "birthdate");
        }
    }

    @Nested
    @DisplayName("RoomCreate")
    class RoomCreateTests {

        @Test
        @DisplayName("é válido com os campos obrigatórios (descrição, imagem, e status são opcionais)")
        void valido() {
            assertThat(validator.validate(new RoomCreate("Suíte", 1L, null, RoomType.SUITE, 250f, null, null))).isEmpty();
        }

        @Test
        @DisplayName("nome em branco, hotel e tipo nulos são recusados")
        void camposObrigatorios() {
            assertThat(camposInvalidos(new RoomCreate(" ", null, null, null, 250f, null, null)))
                    .containsExactlyInAnyOrder("name", "hotelId", "roomType");
        }

        @Test
        @DisplayName("o preço deve ser positivo")
        void precoNegativoOuZero() {
            assertThat(mensagens(new RoomCreate("Q", 1L, null, RoomType.SINGLE, 0f, null, null)))
                    .containsExactly("O valor deve ser positivo");
            assertThat(mensagens(new RoomCreate("Q", 1L, null, RoomType.SINGLE, null, null, null)))
                    .containsExactly("O preço deve ser preenchido");
        }
    }

    @Nested
    @DisplayName("Login e troca de senha")
    class LoginETrocaDeSenha {

        @Test
        @DisplayName("LoginDate exige e-mail e senha preenchidos")
        void loginData() {
            assertThat(validator.validate(new LoginData("a@b.com", "senha"))).isEmpty();
            assertThat(camposInvalidos(new LoginData("", " "))).containsExactlyInAnyOrder("email", "password");
        }

        @Test
        @DisplayName("UpdatedPassword exige senha atual, nova senha e confirmação")
        void updatePassword() {
            assertThat(validator.validate(new UpdatePassword("a", "b", "b"))).isEmpty();
            assertThat(camposInvalidos(new UpdatePassword("", null, "")))
                    .containsExactlyInAnyOrder("currentPassword", "newPassword", "confirmNewPassword");
        }
    }
}
