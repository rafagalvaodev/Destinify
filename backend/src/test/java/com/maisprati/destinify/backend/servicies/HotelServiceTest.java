package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelResponse;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelUpdate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.domain.enums.RoomStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import com.maisprati.destinify.backend.exceptions.HotelNotFoundException;
import com.maisprati.destinify.backend.repositories.HotelRepository;
import com.maisprati.destinify.backend.repositories.RoomRepository;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.utils.mappers.HotelMapper;
import com.maisprati.destinify.backend.utils.mappers.RoomMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Testes do HotelService

@ExtendWith(MockitoExtension.class)
@DisplayName("HotelService")
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelMapper hotelMapper;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private HotelService hotelService;

    private final Pageable pageable = PageRequest.of(0, 10);

    private HotelResponse responseFor(Hotel hotel) {
        return new HotelResponse(hotel.getHotel_id(), hotel.getName(), hotel.getCity(),
                hotel.getAddress(), hotel.getDescription(), hotel.getStars(), hotel.getImageUrl());
    }

    // listAll

    @Test
    @DisplayName("listAll converte cada hotel da página em HotelResponse e mantém os dados da paginação")
    void listAll_convertePaginaInteira() {
        Hotel h1 = TestDataFactory.hotel(1L);
        Hotel h2 = TestDataFactory.hotel(2L);
        HotelResponse r1 = responseFor(h1);
        HotelResponse r2 = responseFor(h2);

        Page<Hotel> page = new PageImpl<>(List.of(h1, h2), pageable, 2);
        when(hotelRepository.findAll(pageable)).thenReturn(page);
        doReturn(r1).when(hotelMapper).hotelResponseMapper(h1);
        doReturn(r2).when(hotelMapper).hotelResponseMapper(h2);

        Page<HotelResponse> result = hotelService.listAll(pageable);

        assertThat(result.getContent()).containsExactly(r1, r2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    // findById

    @Test
    @DisplayName("findById devolve o hotel convertido")
    void findById_existe_retornaResposta() {
        Hotel hotel = TestDataFactory.hotel(1L);
        HotelResponse response = responseFor(hotel);
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));
        when(hotelMapper.hotelResponseMapper(hotel)).thenReturn(response);

        assertThat(hotelService.findById(1L)).isEqualTo(response);
    }

    @Test
    @DisplayName("findById lança HotelNotFoundException quando não existe")
    void findById_naoExiste_lancaExcecao() {
        when(hotelRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.findById(5L))
                .isInstanceOf(HotelNotFoundException.class)
                .hasMessage("Hotel with id 5 not found");
    }

    // create

    @Test
    @DisplayName("create converte o DTO, salva e devolve a resposta do hotel salvo")
    void create_salvaEDevolveResposta() {
        HotelCreate dto = new HotelCreate("Hotel Mar", "Santos", "Av. Praia, 10", "Vista", 350.0, 4.0f, "img");
        Hotel entidade = new Hotel();               // o que o mapper devolve (sem id)
        Hotel salvo = TestDataFactory.hotel(7L);    // o que o banco devolve (com id)
        HotelResponse response = responseFor(salvo);

        when(hotelMapper.hotelCreateMapper(dto)).thenReturn(entidade);
        when(hotelRepository.save(entidade)).thenReturn(salvo);
        when(hotelMapper.hotelResponseMapper(salvo)).thenReturn(response);

        HotelResponse result = hotelService.create(dto);

        assertThat(result).isEqualTo(response);
    }

    // update

    @Test
    @DisplayName("update sobrescreve todos os campos do hotel existente")
    void update_atualizaTodosOsCampos() {
        Hotel existente = TestDataFactory.hotel(1L);
        HotelUpdate dto = new HotelUpdate("Novo Nome", "Nova Cidade", "Novo Endereço", "Nova descrição", 5.0f, "nova-img");
        HotelResponse response = responseFor(existente);

        when(hotelRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(hotelRepository.save(existente)).thenReturn(existente);
        when(hotelMapper.hotelResponseMapper(existente)).thenReturn(response);

        HotelResponse result = hotelService.update(1L, dto);

        assertThat(result).isEqualTo(response);
        assertThat(existente.getName()).isEqualTo("Novo Nome");
        assertThat(existente.getCity()).isEqualTo("Nova Cidade");
        assertThat(existente.getAddress()).isEqualTo("Novo Endereço");
        assertThat(existente.getDescription()).isEqualTo("Nova descrição");
        assertThat(existente.getStars()).isEqualTo(5.0f);
        assertThat(existente.getImageUrl()).isEqualTo("nova-img");
    }

    // NOVO
    @Test
    @DisplayName("update lança HotelNotFoundException e não salva nada quando o hotel não existe")
    void update_naoExiste_lancaExcecao() {
        when(hotelRepository.findById(9L)).thenReturn(Optional.empty());
        HotelUpdate dto = new HotelUpdate("N", "C", "A", "D", 3.0f, "i");

        assertThatThrownBy(() -> hotelService.update(9L, dto))
                .isInstanceOf(HotelNotFoundException.class);

        verify(hotelRepository, never()).save(any(Hotel.class));
    }

    // delete

    @Test
    @DisplayName("delete remove o hotel existente")
    void delete_existe_remove() {
        Hotel hotel = TestDataFactory.hotel(1L);
        when(hotelRepository.findById(1L)).thenReturn(Optional.of(hotel));

        hotelService.delete(1L);

        verify(hotelRepository).delete(hotel);
    }

    @Test
    @DisplayName("delete lança HotelNotFoundException quando o hotel não existe")
    void delete_naoExiste_lancaExcecao() {
        when(hotelRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hotelService.delete(9L))
                .isInstanceOf(HotelNotFoundException.class);

        verify(hotelRepository, never()).delete(any(Hotel.class));
    }

    // findRoomsByHotelId

    @Test
    @DisplayName("findRoomsByHotelId devolve os quartos do hotel convertidos")
    void findRoomsByHotelId_hotelExiste_retornaQuartos() {
        Hotel hotel = TestDataFactory.hotel(1L);
        Room room = TestDataFactory.room(100L, hotel, RoomType.SUITE, 250f);
        RoomResponse roomResponse = new RoomResponse(100L, 1L, "Quarto 100", "Descrição do quarto",
                RoomType.SUITE, 250f, 2, "img", RoomStatus.AVALIABLE);

        when(hotelRepository.existsById(1L)).thenReturn(true);
        when(roomRepository.findRoomsByHotelId(1L, pageable)).thenReturn(new PageImpl<>(List.of(room), pageable, 1));
        when(roomMapper.roomResponseMapper(room)).thenReturn(roomResponse);

        Page<RoomResponse> result = hotelService.findRoomsByHotelId(1L, pageable);

        assertThat(result.getContent()).containsExactly(roomResponse);
    }

    @Test
    @DisplayName("findRoomsByHotelId lança HotelNotFoundException sem consultar os quartos")
    void findRoomsByHotelId_hotelNaoExiste_lancaExcecao() {
        when(hotelRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> hotelService.findRoomsByHotelId(1L, pageable))
                .isInstanceOf(HotelNotFoundException.class)
                .hasMessage("Hotel with id 1 not found");

        verify(roomRepository, never()).findRoomsByHotelId(anyLong(), any(Pageable.class));
    }

    // findByNameAndOrCity (busca com filtros opcionais)

    @Test
    @DisplayName("a busca remove espaços das pontas do nome e da cidade")
    void findByNameAndOrCity_aparaOsEspacos() {
        Hotel hotel = TestDataFactory.hotel(1L);
        HotelResponse response = responseFor(hotel);
        // o stub só casa se o service enviar "Hotel Central" e "Rio" sem os espaços.
        when(hotelRepository.searchIgnoringAccents("Hotel Central", "Rio", pageable))
                .thenReturn(new PageImpl<>(List.of(hotel), pageable, 1));
        when(hotelMapper.hotelResponseMapper(hotel)).thenReturn(response);

        Page<HotelResponse> result = hotelService.findByNameAndOrCity(" Hotel Central ", " Rio ", pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @ParameterizedTest(name = "nome [{0}] vira null (sem filtro de nome)")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    @DisplayName("nome nulo/vazio/em branco vira null e filtra só pela cidade")
    void findByNameAndOrCity_nomeEmBranco_viraNull(String nome) {
        when(hotelRepository.searchIgnoringAccents(null, "Rio", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<HotelResponse> result = hotelService.findByNameAndOrCity(nome, "Rio", pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @ParameterizedTest(name = "cidade [{0}] vira null (sem filtro de cidade)")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "  "})
    @DisplayName("cidade nula/vazia/em branco vira null e filtra só pelo nome")
    void findByNameAndOrCity_cidadeEmBranco_viraNull(String cidade) {
        when(hotelRepository.searchIgnoringAccents("Central", null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<HotelResponse> result = hotelService.findByNameAndOrCity("Central", cidade, pageable);

        assertThat(result.getContent()).isEmpty();
    }

}