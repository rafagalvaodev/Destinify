package com.maisprati.destinify.backend.utils.mappers;

import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomCreate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.domain.enums.RoomStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import com.maisprati.destinify.backend.support.TestDataFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RoomMapper")
class RoomMapperTest {

    private RoomMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RoomMapper();
    }

    @Test
    @DisplayName("roomCreateMapper copia os campos, mas deixa o hotel nulo (o service é quem associa)")
    void roomCreateMapper_copiaCamposEDeixaHotelNulo() {
        RoomCreate dto = new RoomCreate("Suíte Master", 1L, "Com varanda",
                RoomType.SUITE, 450.0f, "http://img/suite.png", RoomStatus.MAINTENANCE);

    Room room = mapper.roomCreateMapper(dto);

    assertThat(room.getName()).isEqualTo("Suíte Master");
    assertThat(room.getDescription()).isEqualTo("Com varanda");
    assertThat(room.getRoomType()).isEqualTo(RoomType.SUITE);
    assertThat(room.getPrice()).isEqualTo(450.0f);
    assertThat(room.getImgUrl()).isEqualTo("http://img/suite.png");
    assertThat(room.getRoomStatus()).isEqualTo(RoomStatus.MAINTENANCE);
    assertThat(room.getHotel()).isNull(); // hotelId do DTO não vira hotel aqui
    }

    @Test
    @DisplayName("roomCreateMapper devolve null quando recebe null")
    void roomCreateMapper_comNull_retornaNull() {
        assertThat(mapper.roomCreateMapper(null)).isNull();
    }

    @Test
    @DisplayName("roomResponseMapper inclui o id do hotel e a capacidade máxima do tipo")
    void roomResponseMapper_incluiHotelIdECapacidade() {
        Room room = TestDataFactory.room(5L, TestDataFactory.hotel(9L), RoomType.TRIPLE, 200f);

        RoomResponse response = mapper.roomResponseMapper(room);

        assertThat(response.room_id()).isEqualTo(5L);
        assertThat(response.hotel_id()).isEqualTo(9L);
        assertThat(response.name()).isEqualTo("Quarto 5");
        assertThat(response.roomType()).isEqualTo(RoomType.TRIPLE);
        assertThat(response.price()).isEqualTo(200f);
        assertThat(response.maxCapacity()).isEqualTo(3); // vem do RoomType.TRIPLE
        assertThat(response.roomStatus()).isEqualTo(RoomStatus.AVALIABLE);
    }
}