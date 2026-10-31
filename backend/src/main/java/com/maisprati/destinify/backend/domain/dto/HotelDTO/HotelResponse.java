package com.maisprati.destinify.backend.domain.dto.HotelDTO;

public record HotelResponse (
        Long hotel_id,
        String name,
        String city,
        String address,
        String description,
        Float stars,
        String imageUrl,
        java.util.List<com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse> suggestedRooms
) {
    public HotelResponse(Long hotel_id, String name, String city, String address,
                         String description, Float stars, String imageUrl) {
        this(hotel_id, name, city, address, description, stars, imageUrl, java.util.List.of());
    }
}
