package com.maisprati.destinify.backend.repositories;

import com.maisprati.destinify.backend.domain.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    @Query("""
        SELECT room 
        FROM Room room 
        WHERE room.hotel.hotel_id = :hotelId
    """)
    Page<Room> findRoomsByHotelId(@Param("hotelId") Long hotelId, Pageable pageable);
}
