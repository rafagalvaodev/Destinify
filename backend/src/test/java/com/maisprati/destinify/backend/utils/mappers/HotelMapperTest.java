package com.maisprati.destinify.backend.utils.mappers;

import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelResponse;
import com.maisprati.destinify.backend.support.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Mapper de Hotel. Mesma ideia do UserMapperTest: função pura, sem Spring,

@DisplayName("HotelMapper")
class HotelMapperTest {
    private HotelMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new HotelMapper();
    }

    @Test
    @DisplayName("hotelCreateMapper copia os campos do DTO para a entidade")
    void hotelCreateMapper_copiaOsCampos() {
        HotelCreate dto = new HotelCreate("Hotel Mar", "Santos", "Av. Praia, 10",
                "Vista para o mar", 350.00, 4.0f, "http://img/mar.png");

        Hotel hotel = mapper.hotelCreateMapper(dto);

        assertThat(hotel.getHotel_id()).isNull(); // o id é gerado pelo banco
        assertThat(hotel.getName()).isEqualTo("Hotel Mar");
        assertThat(hotel.getCity()).isEqualTo("Santos");
        assertThat(hotel.getAddress()).isEqualTo("Av. Praia, 10");
        assertThat(hotel.getDescription()).isEqualTo("Vista para o mar");
        assertThat(hotel.getStars()).isEqualTo(4.0f);
        assertThat(hotel.getImageUrl()).isEqualTo("http://img/mar.png");
    }

    @Test
    @DisplayName("hotelCreateMapper devolve null quando recebe null")
    void hotelCreateMapper_comNull_retornaNull() {
        assertThat(mapper.hotelCreateMapper(null)).isNull();
    }

    @Test
    @DisplayName("hotelResponseMapper converte a entidade em resposta")
    void hotelResponseMapper_copiaOsCampos() {
        Hotel hotel = TestDataFactory.hotel(3L);

        HotelResponse response = mapper.hotelResponseMapper(hotel);

        assertThat(response.hotel_id()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("Hotel Central");
        assertThat(response.city()).isEqualTo("Rio de Janeiro");
        assertThat(response.address()).isEqualTo("Rua A, 100");
        assertThat(response.description()).isEqualTo("Um hotel de teste");
        assertThat(response.stars()).isEqualTo(4.5f);
        assertThat(response.imageUrl()).isEqualTo("http://img/hotel.png");
    }
}
