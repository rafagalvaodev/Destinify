package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelCreate;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelUpdate;
import com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse;
import com.maisprati.destinify.backend.exceptions.HotelNotFoundException;
import com.maisprati.destinify.backend.repositories.HotelRepository;
import com.maisprati.destinify.backend.repositories.RoomRepository;
import com.maisprati.destinify.backend.utils.mappers.HotelMapper;
import com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelResponse;
import com.maisprati.destinify.backend.utils.mappers.RoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.enums.RoomStatus;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    @Transactional(readOnly = true)
    public Page<HotelResponse> findByNameAndOrCity(String name, String city, Pageable pageable){
        String nameFilter = name == null || name.isBlank() ? null : name.trim();
        String cityFilter = city == null || city.isBlank() ? null : city.trim();
        return hotelRepository.searchIgnoringAccents(nameFilter, cityFilter, pageable)
                .map(hotelMapper::hotelResponseMapper);
    }
    @Transactional(readOnly = true)
    public Page<HotelResponse> search(String name, String city, Integer guests, Integer rooms,
                                      LocalDate startDate, LocalDate endDate,
                                      Pageable pageable) {
        if ((guests != null && (guests < 1 || guests > 60))
                || (rooms != null && (rooms < 1 || rooms > 30))
                || (startDate == null) != (endDate == null)
                || (startDate != null && (startDate.isBefore(LocalDate.now())
                    || !endDate.isAfter(startDate)))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe 1 a 60 hóspedes, 1 a 30 quartos e datas válidas (saída após chegada).");
        }
        if (guests == null && rooms == null && startDate == null) {
            return findByNameAndOrCity(name, city, pageable);
        }
        var hotels = hotelRepository.searchIgnoringAccents(
                name == null || name.isBlank() ? null : name.trim(),
                city == null || city.isBlank() ? null : city.trim(),
                Pageable.unpaged(pageable.getSort()));
        var matches = new ArrayList<HotelResponse>();
        for (Hotel hotel : hotels) {
            // With no dates, check capacity only; confirmation always revalidates availability.
            var available = startDate == null
                    ? roomRepository.findRoomsByHotelId(hotel.getHotel_id(),
                        Pageable.unpaged()).getContent().stream()
                        .filter(r -> r.getRoomStatus() == RoomStatus.AVALIABLE).toList()
                    : roomRepository.findAvailableForSearch(hotel.getHotel_id(), startDate, endDate);
            var suggested = suggestRooms(available, guests == null ? 1 : guests, rooms == null ? 1 : rooms);
            if (suggested.isEmpty()) continue;
            var h = hotelMapper.hotelResponseMapper(hotel);
            matches.add(new HotelResponse(h.hotel_id(), h.name(), h.city(), h.address(),
                    h.description(), h.stars(), h.imageUrl(),
                    suggested.stream().map(roomMapper::roomResponseMapper).toList()));
        }
        int from = (int) Math.min(pageable.getOffset(), matches.size());
        int to = Math.min(from + pageable.getPageSize(), matches.size());
        return new PageImpl<>(matches.subList(from, to), pageable, matches.size());
    }

    static List<Room> suggestRooms(
            List<Room> available, int guests, int minimumRooms) {
        var sorted = available.stream().sorted(Comparator
                .comparingInt((Room r) -> r.getRoomType().getMaxCapacity())
                .reversed().thenComparing(Room::getRoom_id)).toList();
        var chosen = new ArrayList<Room>();
        int capacity = 0;
        for (var room : sorted) {
            if (capacity >= guests && chosen.size() >= minimumRooms) break;
            chosen.add(room);
            capacity += room.getRoomType().getMaxCapacity();
        }
        return capacity >= guests && chosen.size() >= minimumRooms ? chosen : List.of();
    }
}

