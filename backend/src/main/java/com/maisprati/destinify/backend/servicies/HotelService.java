package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelUpdate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.exceptions.HotelNotFoundException;
import com.maisprati.destinify.backend.repositories.HotelRepository;
import com.maisprati.destinify.backend.repositories.RoomRepository;
import com.maisprati.destinify.backend.utils.HotelMapper;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelResponse;
import com.maisprati.destinify.backend.utils.RoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelService {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HotelMapper hotelMapper;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomMapper roomMapper;

    @Transactional(readOnly = true)
    public Page<HotelResponse> listAll(Pageable pageable) {
        Page<Hotel> hotelsPage = hotelRepository.findAll(pageable);
        return hotelsPage.map(hotelMapper::hotelResponseMapper);
    }

    @Transactional(readOnly = true)
    public HotelResponse findById(Long id) {
        Hotel hotel = hotelRepository
                .findById(id)
            .orElseThrow(() -> new HotelNotFoundException(id));
        return hotelMapper.hotelResponseMapper(hotel);
    }

    @Transactional
    public HotelResponse create(HotelCreate hotelCreate) {
        Hotel hotel = hotelMapper.hotelCreateMapper(hotelCreate);
        Hotel saved = hotelRepository.save(hotel);
        return hotelMapper.hotelResponseMapper(saved);
    }

    @Transactional
    public HotelResponse update(Long id, HotelUpdate hotelUpdate) {
        Hotel hotel = hotelRepository
                .findById(id)
            .orElseThrow(() -> new HotelNotFoundException(id));
        hotel.setName(hotelUpdate.name());
        hotel.setCity(hotelUpdate.city());
        hotel.setAddress(hotelUpdate.address());
        hotel.setDescription(hotelUpdate.description());
        hotel.setStars(hotelUpdate.stars());
        hotel.setImageUrl(hotelUpdate.imageUrl());

        Hotel updated = hotelRepository.save(hotel);
        return hotelMapper.hotelResponseMapper(updated);
    }

    @Transactional
    public void delete(Long id) {
        Hotel hotel = hotelRepository
                .findById(id)
            .orElseThrow(() -> new HotelNotFoundException(id));

        hotelRepository.delete(hotel);
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> findRoomsByHotelId(Long hotelId, Pageable pageable) {
        if(!hotelRepository.existsById(hotelId)) {
            throw new HotelNotFoundException(hotelId);
        }


        return roomRepository.findRoomsByHotelId(hotelId, pageable).map(roomMapper::roomResponseMapper);
    }

}

