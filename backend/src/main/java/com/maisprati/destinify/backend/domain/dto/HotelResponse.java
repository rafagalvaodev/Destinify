package com.maisprati.destinify.backend.domain.dto;

public record HotelResponse (
        Long id,
        String name,
        String city,
        String address,
        String description,
        Double pricePerNight,
        Integer stars,
        String imageUrl
) {}
