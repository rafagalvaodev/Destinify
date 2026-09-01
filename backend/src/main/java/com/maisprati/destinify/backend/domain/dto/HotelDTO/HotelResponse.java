package com.maisprati.destinify.backend.domain.dto.HotelDTO;

public record HotelResponse (
        Long hotel_id,
        String name,
        String city,
        String address,
        String description,
        Float stars,
        String imageUrl
) {}
