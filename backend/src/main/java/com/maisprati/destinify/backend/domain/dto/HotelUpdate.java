package com.maisprati.destinify.backend.domain.dto;

import jakarta.validation.constraints.*;

public record HotelUpdate(
        @NotBlank String name,
        @NotBlank String city,
        @NotBlank String address,
        String description,
        @NotNull @Positive Double pricePerNight,
        @NotNull @Min(1) @Max(5) Integer stars,
        String imageUrl
){}
