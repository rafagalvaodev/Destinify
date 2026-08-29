package com.maisprati.destinify.backend.utils;

import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.dto.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelResponse;
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
        hotel.setPricePerNight(hotelCreate.pricePerNight());
        hotel.setStars(hotelCreate.stars());
        hotel.setImageUrl(hotelCreate.imageUrl());

        return hotel;
    }

    public HotelResponse hotelResponseMapper(Hotel hotel) {
        return new HotelResponse(
        hotel.getId(),
        hotel.getName(),
        hotel.getCity(),
        hotel.getAddress(),
        hotel.getDescription(),
        hotel.getPricePerNight(),
        hotel.getStars(),
        hotel.getImageUrl());
    }

}
