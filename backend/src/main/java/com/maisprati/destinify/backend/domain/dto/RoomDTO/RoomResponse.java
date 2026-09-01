package com.maisprati.destinify.backend.domain.dto.RoomDTO;

import com.maisprati.destinify.backend.domain.enums.RoomType;

public record RoomResponse(
        Long room_id,
        Long hotel_id,
        String name,
        String description,
        RoomType roomType,
        Float price,
        int maxCapacity,
        String imgUrl) {
}
