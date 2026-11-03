package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomCreate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomUpdate;
import com.maisprati.destinify.backend.domain.enums.RoomStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import com.maisprati.destinify.backend.exceptions.RoomNotFoundException;
import com.maisprati.destinify.backend.repositories.HotelRepository;
import com.maisprati.destinify.backend.repositories.RoomRepository;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.utils.mappers.RoomMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Testes do RoomService

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService")
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private HotelRepository hotelRepository;

    @InjectMocks
    private RoomService roomService;

    private RoomResponse responseFor(Room room) {
        return new RoomResponse(room.getRoom_id(), 1L, room.getName(), room.getDescription(),
                room.getRoomType(), room.getPrice(), room.getRoomType().getMaxCapacity(),
                room.getImgUrl(), room.getRoomStatus());
    }
        // new Room

    @Test
    @DisplayName("newRoom associa o hotel e força o status AVALIABE, ignorando o status do DTO")
    void newRoom_associaHotelEForcaStatusDisponivel() {
        // Arrange: o DTO pede status OCCUPIED, mas a regara do service é sempre AVALIABLE
        RoomCreate dto = new RoomCreate("Suíte Master", 1L, "Varanda", RoomType.SUITE, 450.0f, "img", RoomStatus.OCCUPIED);

        Room mapeado = new Room();
        mapeado.setName("Suíte Master");
        mapeado.setRoomStatus(RoomStatus.OCCUPIED); // o mapper copiou o status do DTO

        Hotel hotelReferencia = TestDataFactory.hotel(1L);
        Room salvo = TestDataFactory.room(100L, hotelReferencia, RoomType.SUITE, 450f);
        RoomResponse response = responseFor(salvo);

        when(roomMapper.roomCreateMapper(dto)).thenReturn(mapeado);
        // getReferenceById devolve um "proxy" do hotel sem ir ao banco de dados: perfeito para só ligar a FK
        when(hotelRepository.getReferenceById(1L)).thenReturn(hotelReferencia);
        when(roomRepository.save(any(Room.class))).thenReturn(salvo);
        when(roomMapper.roomResponseMapper(salvo)).thenReturn(response);

        // Act
        RoomResponse result = roomService.newRoom(dto);

        // Assert
        assertThat(result).isEqualTo(response);

        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        verify(roomRepository).save(captor.capture());
        Room persisted = captor.getValue();
        assertThat(persisted.getHotel()).isSameAs(hotelReferencia);
        assertThat(persisted.getRoomStatus()).isEqualTo(RoomStatus.AVALIABLE);
        }

    // deleteRoomById

    @Test
    @DisplayName("deleteRoomById remove o quarto existente")
    void deleteRoomById_existe_remove() {
        Room room = TestDataFactory.room(100L, TestDataFactory.hotel(1L), RoomType.SINGLE, 100f);
        when(roomRepository.findById(100L)).thenReturn(Optional.of(room));

        roomService.deleteRoomById(100L);

        verify(roomRepository).delete(room);
    }

    @Test
    @DisplayName("deleteRoomById não faz nada quando o quarto não existe")
    void deleteRoomById_naoExiste_naoFazNada() {
        when(roomRepository.findById(100L)).thenReturn(Optional.empty());

        roomService.deleteRoomById(100L);

        verify(roomRepository, never()).delete(any(Room.class));
    }

    // findAll / findRoomById

    @Test
    @DisplayName("findAll converte a página de quartos")
    void findAll_convertePagina() {
        Pageable pageable = PageRequest.of(0, 5);
        Room room = TestDataFactory.room(100L, TestDataFactory.hotel(1L), RoomType.SINGLE, 100f);
        RoomResponse response = responseFor(room);

        when(roomRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(room), pageable, 1));
        when(roomMapper.roomResponseMapper(room)).thenReturn(response);

        Page<RoomResponse> result = roomService.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findRoomById devolve o quarto convertido")
    void findRoomById_existe_retornaResposta() {
        Room room = TestDataFactory.room(100L, TestDataFactory.hotel(1L), RoomType.SINGLE, 100f);
        RoomResponse response = responseFor(room);
        when(roomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(roomMapper.roomResponseMapper(room)).thenReturn(response);

        assertThat(roomService.findRoomById(100L)).isEqualTo(response);
    }

    @Test
    @DisplayName("findRoomById lança RoomNotFoundException quando não existe")
    void findRoomById_naoExiste_lancaExcecao() {
        when(roomRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.findRoomById(9L))
                .isInstanceOf(RoomNotFoundException.class)
                .hasMessage("Room with id 9 not found");
    }

    // updateRoom

    @Test
    @DisplayName("updateRoom altera só os campos informados e mantém os demais")
    void updateRoom_parcial_alteraApenasOsCamposInformados() {
        Room room = TestDataFactory.room(100L, TestDataFactory.hotel(1L), RoomType.SINGLE, 100f);
        // só nome e preços vêm preenchidos; o resto é null (= "não mexa")
        RoomUpdate dto = new RoomUpdate("Nome Novo", null, null, 180f, null, null);

        when(roomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(roomRepository.save(room)).thenReturn(room);
        when(roomMapper.roomResponseMapper(room)).thenReturn(responseFor(room));

        roomService.updateRoom(100L, dto);

        // mudou:
        assertThat(room.getName()).isEqualTo("Nome Novo");
        assertThat(room.getPrice()).isEqualTo(180f);
        // Não mudou:
        assertThat(room.getDescription()).isEqualTo("Descrição do quarto");
        assertThat(room.getRoomType()).isEqualTo(RoomType.SINGLE);
        assertThat(room.getImgUrl()).isEqualTo("http://img/room.png");
        assertThat(room.getRoomStatus()).isEqualTo(RoomStatus.AVALIABLE);
    }

    @Test
    @DisplayName("updateRoom altera todos os campos quando todos são informados")
    void updateRoom_completo_alteraTudo() {
        Room room = TestDataFactory.room(100L, TestDataFactory.hotel(1L), RoomType.SINGLE, 100f);
        RoomUpdate dto = new RoomUpdate("Novo", "Nova descrição", RoomType.TRIPLE, 500f,
                "nova-img", RoomStatus.MAINTENANCE);

        when(roomRepository.findById(100L)).thenReturn(Optional.of(room));
        when(roomRepository.save(room)).thenReturn(room);
        when(roomMapper.roomResponseMapper(room)).thenReturn(responseFor(room));

        roomService.updateRoom(100L, dto);

        assertThat(room.getName()).isEqualTo("Novo");
        assertThat(room.getDescription()).isEqualTo("Nova descrição");
        assertThat(room.getRoomType()).isEqualTo(RoomType.TRIPLE);
        assertThat(room.getPrice()).isEqualTo(500f);
        assertThat(room.getImgUrl()).isEqualTo("nova-img");
        assertThat(room.getRoomStatus()).isEqualTo(RoomStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("updateRoom lança RoomNotFoundException e não salva quando o quarto não existe")
    void updateRoom_naoExiste_lancaExcecao() {
        when(roomRepository.findById(9L)).thenReturn(Optional.empty());
        RoomUpdate dto = new RoomUpdate("x", null, null, null, null, null);

        assertThatThrownBy(() -> roomService.updateRoom(9L, dto))
                .isInstanceOf(RoomNotFoundException.class);

        verify(roomRepository, never()).save(any(Room.class));
    }

}
