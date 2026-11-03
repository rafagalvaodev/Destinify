package com.maisprati.destinify.backend.repositories;

import com.maisprati.destinify.backend.domain.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    @Query("""
        SELECT room 
        FROM Room room 
        WHERE room.hotel.hotel_id = :hotelId
    """)
    Page<Room> findRoomsByHotelId(@Param("hotelId") Long hotelId, Pageable pageable);

    @Query("""
        SELECT r FROM Room r
        WHERE r.hotel.hotel_id = :hotelId
        AND r.roomStatus = com.maisprati.destinify.backend.domain.enums.RoomStatus.AVALIABLE
        AND NOT EXISTS (SELECT b.id FROM Booking b JOIN b.rooms br
            WHERE br.room_id = r.room_id
            AND b.status <> com.maisprati.destinify.backend.domain.enums.BookingStatus.CANCELLED
            AND b.checkInDate < :endDate AND b.checkOutDate > :startDate)
        """)
    java.util.List<Room> findAvailableForSearch(Long hotelId, java.time.LocalDate startDate,
                                               java.time.LocalDate endDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.room_id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
