package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelResponse;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelUpdate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.servicies.HotelService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    // Lead -> Listar
    @GetMapping
    public ResponseEntity<Page<HotelResponse>> listAll(Pageable pageable) {
        Page<HotelResponse> hotelResponsePage = hotelService.listAll(pageable);
        return ResponseEntity.ok(hotelResponsePage);
    }

    // Ver detelhes do hotel
    @GetMapping("/{id}")
    public ResponseEntity<HotelResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(hotelService.findById(id));
    }

    // Create -> Criar
    @PostMapping
    public ResponseEntity<HotelResponse> create(@Valid @RequestBody HotelCreate hotelCreate) {
        HotelResponse response = hotelService.create(hotelCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Update -> Atualizar
    @PutMapping("/{id}")
    public ResponseEntity<HotelResponse> update(@PathVariable Long id, @Valid @RequestBody HotelUpdate hotelUpdate) {
        return ResponseEntity.ok(hotelService.update(id, hotelUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hotelService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/rooms")
    public ResponseEntity<Page<RoomResponse>> findRoomsByHotelId(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(hotelService.findRoomsByHotelId(id, pageable));
    }
}