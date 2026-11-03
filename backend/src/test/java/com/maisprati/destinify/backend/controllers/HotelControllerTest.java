package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelResponse;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelUpdate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.domain.enums.RoomStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import com.maisprati.destinify.backend.exceptions.HotelNotFoundException;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.HotelService;
import com.maisprati.destinify.backend.servicies.TokenService;
import com.maisprati.destinify.backend.support.TestAuth;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.support.WebTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HotelController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("HotelController")
public class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HotelService hotelService;

    @MockitoBean
    private TokenService tokenService;          // exigido pelo AccessTokenFilter

    @MockitoBean
    private UserRepository userRepository;      // exigido pelo accessTokenFilter

    private static final String HOTEL_JSON = """
            {"name":"Hotel Mar", "city":"Santos", "address":"Av. Praia, 10",
            "description":"Vista", "pricePerNight":350.0, "stars":4.0, "imageUrl": "img"}
            """;

    private static final String HOTEL_UPDATE_JSON = """
            {"name":"Hotel Novo", "city":"Santos", "address":"Av. Praia, 10",
            "description":"Vista", "stars":5.0, "imageUrl":"img"}
            """;

    private HotelResponse hotelResponse() {
        return new HotelResponse(1L, "Hotel Central", "Rio de Janeiro", "Rua A, 100", "Um hotel", 4.5f, "img");
    }

    // Rotas Públicas (GET) - qualquer um acessa, mesmo sem login

    @Nested
    @DisplayName("rotas públicas")
    class RotasPublicas {

        @Test
        @DisplayName("GET /api/hotels lista os hóteis sem exigir login")
        void listAll_semLogin_retorna200() throws Exception {
            when(hotelService.listAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(hotelResponse()), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/hotels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].hotel_id").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Hotel Central"));
        }

        @Test
        @DisplayName("GET /api/hotels repassa page e size da URL para o service")
        void listAll_repassaParametrosDePaginacao() throws Exception {
            when(hotelService.listAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

            mockMvc.perform(get("/api/hotels").param("page", "2").param("size", "5"))
                    .andExpect(status().isOk());

            // ArgumentCaptor: descobre com qual Pageable o controller chamou o service
            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            verify(hotelService).listAll(captor.capture());
            assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
            assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        }

        @Test
        @DisplayName("GET /api/hotels/{id} devolve o hotel")
        void findById_retorna200() throws Exception {
            when(hotelService.findById(1L)).thenReturn(hotelResponse());

            mockMvc.perform(get("/api/hotels/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Hotel Central"))
                    .andExpect(jsonPath("$.stars").value(4.5));
        }

        @Test
        @DisplayName("GET /api/hotels/{id} devolve 404 com ApiError quando o hotel não existe")
        void findById_naoExiste_retorna404ComApiError() throws Exception {
            // Aqui o service "explode" com a exceção, e o GlobalExceptionHandler a traduz em JSON
            when(hotelService.findById(99L)).thenThrow(new HotelNotFoundException(99L));

            mockMvc.perform(get("/api/hotels/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Hotel with id 99 not found"))
                    .andExpect(jsonPath("$.path").value("/api/hotels/99"));
        }

        @Test
        @DisplayName("GET /api/hotels/search repassa nome e cidade para o service")
        void search_repassaFiltros() throws Exception {
            when(hotelService.findByNameAndOrCity(eq("rio"), eq("RJ"), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(hotelResponse()), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/hotels/search").param("name", "rio").param("city", "RJ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Hotel Central"));
        }

        @Test
        @DisplayName("GET /api/hotels/search funciona sem nenhum filtro (ambos opcionais)")
        void search_semFiltros_retorna200() throws Exception {
            when(hotelService.findByNameAndOrCity(isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

            mockMvc.perform(get("/api/hotels/search"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /api/hotels/{id}/rooms lista os quartos do hotel")
        void findRooms_retorna200() throws Exception {
            RoomResponse room = new RoomResponse(100L, 1L, "Quarto 100", "desc", RoomType.SUITE, 250f, 2, "img", RoomStatus.AVALIABLE);
            when(hotelService.findRoomsByHotelId(eq(1L), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(room), PageRequest.of(0, 10), 1));

            mockMvc.perform(get("/api/hotels/1/rooms"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].room_id").value(100))
                    .andExpect(jsonPath("$.content[0].maxCapacity").value(2));
        }
    }

    // Criar Hotel: só ADMIN

    @Nested
    @DisplayName("POST /api/hotels (somente ADMIN)")
    class CriarHotel {

        @Test
        @DisplayName("ADMIN cria o hotel e recebe 201 com o corpo")
        void admin_retorna201() throws Exception {
            when(hotelService.create(any(HotelCreate.class))).thenReturn(hotelResponse());

            mockMvc.perform(post("/api/hotels")
                    .with(TestAuth.as(TestDataFactory.admin(1L)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(HOTEL_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Hotel Central"));

            // Confirma que o JSON virou o objeto HotelCreate esperado (record tem equals automático)
            verify(hotelService).create(new HotelCreate("Hotel Mar", "Santos", "Av. Praia, 10",
                    "Vista", 350.0, 4.0f, "img"));
        }

        @Test
        @DisplayName("CLIENT recebe 403")
        void client_retorna403() throws Exception {
            mockMvc.perform(post("/api/hotels")
                    .with(TestAuth.as(TestDataFactory.client(2L)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(HOTEL_JSON))
                .andExpect(status().isForbidden());

            verify(hotelService, never()).create(any(HotelCreate.class)); // nem chegou ao service
        }

        @Test
        @DisplayName("sem login recebe 403")
        void semLogin_retorna403() throws Exception {
            mockMvc.perform(post("/api/hotels")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(HOTEL_JSON))
                .andExpect(status().isForbidden());
            verify(hotelService, never()).create(any(HotelCreate.class));
        }

        @Test
        @DisplayName("JSON inválido (nome em branco e estrelas 9) recebe 400 e o service não é chamado")
        void jsonInvalido_retorna400() throws Exception {
            String invalido = """
                    {"name":" ", "city":"Santos", "address":"Rua", "pricePerNight":"100.0", "stars":9.0}
                    """;

            mockMvc.perform(post("/api/hotels")
                    .with(TestAuth.as(TestDataFactory.admin(1L)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalido))
                .andExpect(status().isBadRequest());

            verify(hotelService, never()).create(any(HotelCreate.class)); // @Valid barrou antes
        }
    }

    // Atualizar HOTEL: só ADMIN

    @Nested
    @DisplayName("PUT /api/hotels/{id} (somente ADMIN)")
    class AtualizarHotel {

        @Test
        @DisplayName("ADMIN atualiza e recebe 200")
        void admin_retorna200() throws Exception {
            when(hotelService.update(eq(1L), any(HotelUpdate.class))).thenReturn(hotelResponse());

            mockMvc.perform(put("/api/hotels/1")
                    .with(TestAuth.as(TestDataFactory.admin(1L)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(HOTEL_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotel_id").value(1));
        }

        @Test
        @DisplayName("CLIENT recebe 403")
        void client_retorna403() throws Exception {
            mockMvc.perform(put("/api/hotels/1")
                    .with(TestAuth.as(TestDataFactory.client(2L)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(HOTEL_UPDATE_JSON))
                .andExpect(status().isForbidden());

            verify(hotelService, never()).update(anyLong(), any(HotelUpdate.class));
        }

        @Test
        @DisplayName("hotel inexistente devolve 404")
        void naoExiste_retorna404() throws Exception {
            when(hotelService.update(eq(99L), any(HotelUpdate.class))).thenThrow(new HotelNotFoundException(99L));

            mockMvc.perform(put("/api/hotels/99")
                    .with(TestAuth.as(TestDataFactory.admin(1L)))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(HOTEL_UPDATE_JSON))
                .andExpect(status().isNotFound());
        }
    }

    // Exluir HOTEL: só ADMIN

    @Nested
    @DisplayName("DELETE /api/hotels/{id} (somente ADMIN)")
    class ExcluirHotel {

        @Test
        @DisplayName("ADMIN exlui e recebe 204 sem corpo")
        void admin_retorna204() throws Exception {
            mockMvc.perform(delete("/api/hotels/1").with(TestAuth.as(TestDataFactory.admin(1L))))
                    .andExpect(status().isNoContent());

            verify(hotelService).delete(1L);
        }

        @Test
        @DisplayName("CLIENT recebe 403 e nada é excluído")
        void client_retorna403() throws Exception {
            mockMvc.perform(delete("/api/hotels/1").with(TestAuth.as(TestDataFactory.client(2L))))
                    .andExpect(status().isForbidden());

            verify(hotelService, never()).delete(anyLong());
        }
    }
}
