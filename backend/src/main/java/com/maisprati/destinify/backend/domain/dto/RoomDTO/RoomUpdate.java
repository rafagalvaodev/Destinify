package com.maisprati.destinify.backend.domain.dto.RoomDTO;

import com.maisprati.destinify.backend.domain.enums.RoomStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;

public record RoomUpdate(
        String name,
        String description,
        RoomType roomType,
        Float price,
        String imgUrl,
        RoomStatus roomStatus) {
}
