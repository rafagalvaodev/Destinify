package com.maisprati.destinify.backend.domain.dto.RoomDTO;

import com.maisprati.destinify.backend.domain.enums.RoomType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RoomCreate(
        @NotBlank(message = "O nome deve ser preenchido")
        String name,
        @NotNull(message = "O id do deve ser preenchido")
        Long hotelId,
        String description,
        @NotNull(message = "O tipo deve ser preenchido")
        RoomType roomType,
        @NotNull(message = "O preço deve ser preenchido")
        @Positive(message = "O valor deve ser positivo")
        Float price,
        String imgUrl){
}
