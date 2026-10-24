package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomCreate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomUpdate;
import com.maisprati.destinify.backend.servicies.RoomService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @PostMapping("/newRoom")
    public ResponseEntity<RoomResponse> addNewRoom(
            @Valid @RequestBody RoomCreate roomCreate) {
        RoomResponse roomResponse = roomService.newRoom(roomCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(roomResponse);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<RoomResponse> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomUpdate roomUpdate){

        return ResponseEntity.ok(roomService.updateRoom(id, roomUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoomById(@PathVariable Long id){
        roomService.deleteRoomById(id);
        return ResponseEntity.noContent().build();
    }

    @SecurityRequirements
    @GetMapping("/all")
    public ResponseEntity<Page<RoomResponse>> getAll(Pageable pageable) {
        Page<RoomResponse> roomsResponsePage = roomService.findAll(pageable);
        return ResponseEntity.ok(roomsResponsePage);
    }

    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> getRoomById(@PathVariable Long id){
        return ResponseEntity.ok(roomService.findRoomById(id));
    }
}
