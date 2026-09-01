package com.maisprati.destinify.backend.utils;

import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomCreate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {
    public Room roomCreateMapper(RoomCreate roomCreateDTO){
        if (roomCreateDTO==null) return null;

        Room roomCreate = new Room();

        roomCreate.setName(roomCreateDTO.name());
        roomCreate.setDescription(roomCreateDTO.description());
        roomCreate.setRoomType(roomCreateDTO.roomType());
        roomCreate.setPrice(roomCreateDTO.price());
        roomCreate.setImgUrl(roomCreateDTO.imgUrl());

        roomCreate.setHotel(roomCreate.getHotel());

        return roomCreate;
    }

    public RoomResponse roomResponseMapper(Room room){
        return new RoomResponse(
                room.getRoom_id(),
                room.getHotel().getHotel_id(),
                room.getName(),
                room.getDescription(),
                room.getRoomType(),
                room.getPrice(),
                room.getRoomType().getMaxCapacity(),
                room.getImgUrl());
    }


}
