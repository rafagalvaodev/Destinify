package com.maisprati.destinify.backend.domain.dto.HotelDTO;

import jakarta.validation.constraints.*;

public record HotelCreate(
    @NotBlank String name,
    @NotBlank String city,
    @NotBlank String address,
    String description,
    @NotNull @Positive Double pricePerNight,
    @NotNull @Min(1) @Max(5) Float stars,
    String imageUrl
) {
}
