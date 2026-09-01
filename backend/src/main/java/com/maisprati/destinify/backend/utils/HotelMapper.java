package com.maisprati.destinify.backend.utils;

import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelResponse;
import org.springframework.stereotype.Component;

@Component
public class HotelMapper {

    public Hotel hotelCreateMapper(HotelCreate hotelCreate) {
        if (hotelCreate == null) return null;

        Hotel hotel = new Hotel();
        hotel.setName(hotelCreate.name());
        hotel.setCity(hotelCreate.city());
        hotel.setAddress(hotelCreate.address());
        hotel.setDescription(hotelCreate.description());
        hotel.setStars(hotelCreate.stars());
        hotel.setImageUrl(hotelCreate.imageUrl());

        return hotel;
    }

    public HotelResponse hotelResponseMapper(Hotel hotel) {
        return new HotelResponse(
        hotel.getHotel_id(),
        hotel.getName(),
        hotel.getCity(),
        hotel.getAddress(),
        hotel.getDescription(),
        hotel.getStars(),
        hotel.getImageUrl());
    }

}
