package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomCreate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomUpdate;
import com.maisprati.destinify.backend.domain.enums.RoomStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import com.maisprati.destinify.backend.exceptions.RoomNotFoundException;
import com.maisprati.destinify.backend.repositories.UserRepository;
import com.maisprati.destinify.backend.security.SecurityConfig;
import com.maisprati.destinify.backend.servicies.RoomService;
import com.maisprati.destinify.backend.servicies.TokenService;
import com.maisprati.destinify.backend.support.TestAuth;
import com.maisprati.destinify.backend.support.TestDataFactory;
import com.maisprati.destinify.backend.support.WebTestConfig;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoomController.class)
@Import({SecurityConfig.class, WebTestConfig.class})
@DisplayName("RoomController")
public class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomService roomService;

    @MockitoBean
    private TokenService tokenService;      // exigido pelo AccessTokenFilter

    @MockitoBean
    private UserRepository userRepository;  // exigido pelo AccessTokenFilter

    private static final String ROOM_JSON = """
            {"name":"Suíte Master", "hotelId":1, "description":"Varanda", "roomType":"SUITE", "price":450.0, "imgUrl":"img"}
            """;

    private RoomResponse roomResponse() {
        return new RoomResponse(100L, 1L, "Suíte Master", "Varanda", RoomType.SUITE, 450f, 2, "img", RoomStatus.AVALIABLE);
    }

    // públicas

    @Test
    @DisplayName("GET /api/rooms/all lista os quartos sem exigir login")
    void getAll_semLogin_retorna200() throws Exception {
        when(roomService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(roomResponse()), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/rooms/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].room_id").value(100))
                .andExpect(jsonPath("$.content[0].roomType").value("SUITE"));
    }

    @Test
    @DisplayName("GET /api/rooms/{id} devolve o quarto sem exigir login")
    void getById_semLogin_retorna200() throws Exception {
        when(roomService.findRoomById(100L)).thenReturn(roomResponse());

        mockMvc.perform(get("/api/rooms/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Suíte Master"))
                .andExpect(jsonPath("$.maxCapacity").value(2));
    }

    @Test
    @DisplayName("GET /api/rooms/{id} devolve 404 quando o quarto não existe")
    void getById_naoExiste_retorna404() throws Exception {
        when(roomService.findRoomById(9L)).thenThrow(new RoomNotFoundException(9L));

        mockMvc.perform(get("/api/rooms/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Room with id 9 not found"));
    }

    // criar

    @Test
    @DisplayName("POST /api/rooms/newRoom: ADMIN cria e recebe 201")
    void newRoom_admin_retorna201() throws Exception {
        when(roomService.newRoom(any(RoomCreate.class))).thenReturn(roomResponse());

        mockMvc.perform(post("/api/rooms/newRoom")
                .with(TestAuth.as(TestDataFactory.admin(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(ROOM_JSON))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.room_id").value(100));
    }

    @Test
    @DisplayName("POST /api/rooms/newRoom: CLIENT recebe 403")
    void newRoom_client_retorna403() throws Exception {
        mockMvc.perform(post("/api/rooms/newRoom")
                        .with(TestAuth.as(TestDataFactory.client(2L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROOM_JSON))
                .andExpect(status().isForbidden());

        verify(roomService, never()).newRoom(any(RoomCreate.class));
    }

    @Test
    @DisplayName("POST /api/rooms/newRoom: sem login recebe 403")
    void newRoom_semLogin_retorna403() throws Exception {
        mockMvc.perform(post("/api/rooms/newRoom")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ROOM_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/rooms/newRoom: preço negativo e nome em branco recebem 400")
    void newRoom_dadosInvalidos_retorna400() throws Exception {
        String invalido = """
                {"name":" ", "hotelId":1, "roomType":"SUITE", "price":-5.0}
                """;

        mockMvc.perform(post("/api/rooms/newRoom")
                .with(TestAuth.as(TestDataFactory.admin(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalido))
            .andExpect(status().isBadRequest());

        verify(roomService, never()).newRoom(any(RoomCreate.class));
    }

    // Atualizar

    @Test
    @DisplayName("PATCH /api/rooms/{id}: ADMIN atualiza parcialmente e recebe 200")
    void update_admin_retorna200() throws Exception {
        when(roomService.updateRoom(eq(100L), any(RoomUpdate.class))).thenReturn(roomResponse());

        mockMvc.perform(patch("/api/rooms/100")
                .with(TestAuth.as(TestDataFactory.admin(1L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":500.0}"))
            .andExpect(status().isOk());

        // Só o preço veio no JSON; os demais campos do record chegam como null
        verify(roomService).updateRoom(100L, new RoomUpdate(null, null, null, 500f, null, null));
    }

    @Test
    @DisplayName("PATCH /api/rooms/{id}: CLIENT recebe 403")
    void update_client_retorna403() throws Exception {
        mockMvc.perform(patch("/api/rooms/100")
                .with(TestAuth.as(TestDataFactory.client(2L)))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":500.0}"))
            .andExpect(status().isForbidden());

        verify(roomService, never()).updateRoom(anyLong(), any(RoomUpdate.class));
    }

    // excluir

    @Test
    @DisplayName("DELETE /api/rooms/{id}: ADMIN exclui e recebe 204")
    void delete_admin_retorna204() throws Exception {
        mockMvc.perform(delete("/api/rooms/100")
                .with(TestAuth.as(TestDataFactory.admin(1L))))
            .andExpect(status().isNoContent());

        verify(roomService).deleteRoomById(100L);
    }

    @Test
    @DisplayName("DELETE /api/rooms/{id}: CLIENT recebe 403")
    void delete_client_retorna403() throws Exception {
        mockMvc.perform(delete("/api/rooms/100")
                .with(TestAuth.as(TestDataFactory.client(2L))))
            .andExpect(status().isForbidden());

        verify(roomService, never()).deleteRoomById(anyLong());
    }
}
