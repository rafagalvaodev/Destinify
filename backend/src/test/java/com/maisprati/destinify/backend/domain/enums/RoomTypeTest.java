package com.maisprati.destinify.backend.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoomType (enum)")
class RoomTypeTest {

    @ParameterizedTest(name = "{0} comporta {1} hóspede(s)")
    @CsvSource({
            "SINGLE, 1",
            "SUITE, 2",
            "TRIPLE, 3"
    })
    @DisplayName("cada tipo de quarto tem a capacidade máxima esperada")
    void getMaxCapacity_retornaCapacidadeDoTipo(RoomType type, int expectedCapacity) {
        assertThat(type.getMaxCapacity()).isEqualTo(expectedCapacity);
    }
}
