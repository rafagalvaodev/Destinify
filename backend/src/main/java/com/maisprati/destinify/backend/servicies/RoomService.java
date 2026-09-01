package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomCreate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomUpdate;
import com.maisprati.destinify.backend.exceptions.RoomNotFoundException;
import com.maisprati.destinify.backend.repositories.HotelRepository;
import com.maisprati.destinify.backend.repositories.RoomRepository;
import com.maisprati.destinify.backend.utils.RoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMapper roomMapper;

    @Autowired
    private HotelRepository hotelRepository;

    @Transactional
    public RoomResponse newRoom(RoomCreate roomCreate) {

        Room room = roomMapper.roomCreateMapper(roomCreate);

        room.setHotel(hotelRepository.getReferenceById(roomCreate.hotelId()));

        Room saveRoom = roomRepository.save(room);
        return roomMapper.roomResponseMapper(saveRoom);
    }

    @Transactional
    public void deleteRoomById(Long id) {
        Optional<Room> room = roomRepository.findById(id);
        room.ifPresent(value -> roomRepository.delete(value));
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> findAll(Pageable pageable){

        Page<Room> roomsPage = roomRepository.findAll(pageable);
        return roomsPage.map(roomMapper::roomResponseMapper);
    }

    @Transactional(readOnly = true)
    public RoomResponse findRoomById(Long id){
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));
        return roomMapper.roomResponseMapper(room);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomUpdate roomUpdate){
        Room room = roomRepository
                .findById(id).orElseThrow(() -> new RoomNotFoundException(id));

        if(roomUpdate.name() != null){
            room.setName(roomUpdate.name());
        }

        if(roomUpdate.description() != null){
            room.setDescription(roomUpdate.description());
        }

        if(roomUpdate.roomType() != null) {
            room.setRoomType(roomUpdate.roomType());
        }

        if(roomUpdate.price() != null ) {
            room.setPrice(roomUpdate.price());
        }

        if (roomUpdate.imgUrl() != null) {
            room.setImgUrl(roomUpdate.imgUrl());
        }

        Room saveRoomUpdated = roomRepository.save(room);
        return roomMapper.roomResponseMapper(saveRoomUpdated);
    }

}
