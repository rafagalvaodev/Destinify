package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.dto.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelUpdate;
import com.maisprati.destinify.backend.repositories.HotelRepository;
import com.maisprati.destinify.backend.utils.HotelMapper;
import java.util.List;
import com.maisprati.destinify.backend.domain.dto.HotelResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HotelService {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private HotelMapper hotelMapper;

    @Transactional(readOnly = true)
    public List<HotelResponse> listAll() {
        return hotelRepository.findAll()
            .stream()
            .map(hotelMapper::hotelResponseMapper)
            .toList();
    }
    @Transactional(readOnly = true)
    public HotelResponse findById(Long id) {
        Hotel hotel = hotelRepository
                .findById(id)
            .orElseThrow(() -> new RuntimeException("Hotel not found"));
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
            .orElseThrow(() -> new RuntimeException("Hotel not found"));
        hotel.setName(hotelUpdate.name());
        hotel.setCity(hotelUpdate.city());
        hotel.setAddress(hotelUpdate.address());
        hotel.setDescription(hotelUpdate.description());
        hotel.setPricePerNight(hotelUpdate.pricePerNight());
        hotel.setStars(hotelUpdate.stars());
        hotel.setImageUrl(hotelUpdate.imageUrl());

        Hotel updated = hotelRepository.save(hotel);
        return hotelMapper.hotelResponseMapper(updated);
    }

    @Transactional
    public void delete(Long id) {
        Hotel hotel = hotelRepository
                .findById(id)
            .orElseThrow(() -> new RuntimeException("Hotel not found"));

        hotelRepository.delete(hotel);
    }

}

