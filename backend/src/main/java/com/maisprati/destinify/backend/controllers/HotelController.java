package com.maisprati.destinify.backend.controllers;

import com.maisprati.destinify.backend.domain.dto.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelResponse;
import com.maisprati.destinify.backend.domain.dto.HotelUpdate;
import com.maisprati.destinify.backend.servicies.HotelService;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    // Lead -> Listar
    @GetMapping
    public ResponseEntity<List<HotelResponse>> listAll() {
        return ResponseEntity.ok(hotelService.listAll());
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
}