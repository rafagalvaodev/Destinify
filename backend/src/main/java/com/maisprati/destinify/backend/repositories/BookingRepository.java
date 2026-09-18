package com.maisprati.destinify.backend.repositories;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
    SELECT b
    FROM Booking b
    WHERE b.user.id = :userId
    ORDER BY b.checkInDate DESC
    """)
    Page<Booking> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.room.room_id = :roomId
        AND b.status <> :cancelledStatus
        AND b.checkInDate < :checkOutDate
        AND b.checkOutDate > :checkInDate
""")
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomID,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("cancelledStatus") BookingStatus cancelledStatus
    );

    @Query("""
        SELECT b
        FROM Booking b
        WHERE b.room.room_id = :roomId
        AND b.id <> :bookingId
        AND b.status <> :cancelledStatus
        AND b.checkInDate < :checkOutDate
        AND b.checkOutDate > :checkInDate
    """)
    List<Booking> findOverlappingBookingsExcludingBooking(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("cancelledStatus") BookingStatus cancelledStatus,
            @Param("bookingId") Long bookingId
    );
}
