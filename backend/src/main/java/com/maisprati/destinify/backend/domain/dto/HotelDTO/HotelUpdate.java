package com.maisprati.destinify.backend.domain.dto.HotelDTO;

import jakarta.validation.constraints.*;

public record HotelUpdate(
        @NotBlank String name,
        @NotBlank String city,
        @NotBlank String address,
        String description,
        @NotNull @Min(1) @Max(5) Float stars,
        String imageUrl
){}
