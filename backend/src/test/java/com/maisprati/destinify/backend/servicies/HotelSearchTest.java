package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HotelSearchTest {
    private Room room(long id, RoomType type) {
        Room room = new Room();
        room.setRoom_id(id);
        room.setRoomType(type);
        return room;
    }

    @Test
    void combinesRoomsForFiveGuests() {
        var triple = room(1, RoomType.TRIPLE);
        var suite = room(2, RoomType.SUITE);
        var result = HotelService.suggestRooms(List.of(suite, triple), 5, 1);
        assertEquals(List.of(triple, suite), result);
    }

    @Test
    void rejectsInsufficientCapacity() {
        assertTrue(HotelService.suggestRooms(List.of(room(1, RoomType.TRIPLE)), 5, 1).isEmpty());
    }

    @Test
    void respectsMinimumRoomCountWithoutReusingRooms() {
        var rooms = List.of(room(1, RoomType.TRIPLE), room(2, RoomType.SINGLE));
        assertEquals(2, HotelService.suggestRooms(rooms, 1, 2).size());
        assertTrue(HotelService.suggestRooms(rooms, 1, 3).isEmpty());
    }

    @Test
    void rejectsInvalidOccupancyAndIncompleteOrReversedDates() {
        var service = new HotelService();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 12);
        var tomorrow = java.time.LocalDate.now().plusDays(1);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.search(null, null, 0, 1, null, null, pageable));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.search(null, null, 5, 1, tomorrow, null, pageable));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.search(null, null, 5, 1, tomorrow, tomorrow, pageable));
    }

    @Test
    void searchFiltersHotelsBeforePagination() {
        var hotels = org.mockito.Mockito.mock(com.maisprati.destinify.backend.repositories.HotelRepository.class);
        var rooms = org.mockito.Mockito.mock(com.maisprati.destinify.backend.repositories.RoomRepository.class);
        var hotelMapper = org.mockito.Mockito.mock(com.maisprati.destinify.backend.utils.mappers.HotelMapper.class);
        var roomMapper = org.mockito.Mockito.mock(com.maisprati.destinify.backend.utils.mappers.RoomMapper.class);
        var service = new HotelService();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "hotelRepository", hotels);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "roomRepository", rooms);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "hotelMapper", hotelMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "roomMapper", roomMapper);
        var small = new com.maisprati.destinify.backend.domain.Hotel();
        small.setHotel_id(1L);
        var large = new com.maisprati.destinify.backend.domain.Hotel();
        large.setHotel_id(2L);
        var single = room(1, RoomType.SINGLE);
        single.setRoomStatus(com.maisprati.destinify.backend.domain.enums.RoomStatus.AVALIABLE);
        var triple = room(2, RoomType.TRIPLE);
        triple.setRoomStatus(com.maisprati.destinify.backend.domain.enums.RoomStatus.AVALIABLE);
        org.mockito.Mockito.when(hotels.searchIgnoringAccents(org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(small, large)));
        org.mockito.Mockito.when(rooms.findRoomsByHotelId(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(single)));
        org.mockito.Mockito.when(rooms.findRoomsByHotelId(org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(triple)));
        org.mockito.Mockito.when(hotelMapper.hotelResponseMapper(large)).thenReturn(
                new com.maisprati.destinify.backend.domain.dto.HotelDTO.HotelResponse(2L, "Grande", "Cidade", "Rua", "", 3f, ""));
        org.mockito.Mockito.when(roomMapper.roomResponseMapper(triple)).thenReturn(
                new com.maisprati.destinify.backend.domain.dto.RoomDTO.RoomResponse(2L, 2L, "Triplo", "", RoomType.TRIPLE, 100f, 3, "", triple.getRoomStatus()));
        var result = service.search(null, null, 3, 1, null, null,
                org.springframework.data.domain.PageRequest.of(0, 1));
        assertEquals(1, result.getTotalElements());
        assertEquals(2L, result.getContent().getFirst().hotel_id());
        assertTrue(service.search(null, null, 10, 1, null, null,
                org.springframework.data.domain.PageRequest.of(0, 12)).isEmpty());
    }
}
