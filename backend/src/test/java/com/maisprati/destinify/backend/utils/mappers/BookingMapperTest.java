package com.maisprati.destinify.backend.utils.mappers;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingResponse;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import com.maisprati.destinify.backend.support.TestDataFactory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Mapper de Booking

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingMapper")
class BookingMapperTest {

    @Spy
    private RoomMapper roomMapper = new RoomMapper();

    @InjectMocks
    private BookingMapper bookingMapper;

    @Test
    @DisplayName("bookingResponseMapper copia os dados da reserva e converte cada quarto")
    void bookingResponseMapper_copiaDadosEConverteQuartos() {
        // Arrange
        User user = TestDataFactory.client(1L);
        Hotel hotel = TestDataFactory.hotel(10L);
        Room room1 = TestDataFactory.room(100L, hotel, RoomType.SINGLE, 100f);
        Room room2 = TestDataFactory.room(101L, hotel, RoomType.SUITE, 200f);
        LocalDate checkIn = LocalDate.of(2030, 6, 1);
        LocalDate checkOut = LocalDate.of(2030, 6, 4);
        Booking booking = TestDataFactory.booking(55L, user, hotel, List.of(room1, room2),
                checkIn, checkOut, BookingStatus.PENDING);
        booking.setGuestsCount(3);
        booking.setTotalPrice(new BigDecimal("900.00"));

        // Act
        BookingResponse response = bookingMapper.bookingResponseMapper(booking);

        // Assert
        assertThat(response.id()).isEqualTo(55L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.userName()).isEqualTo("Maria Cliente");
        assertThat(response.hotelId()).isEqualTo(10L);
        assertThat(response.hotelName()).isEqualTo("Hotel Central");
        assertThat(response.checkInDate()).isEqualTo(checkIn);
        assertThat(response.checkOutDate()).isEqualTo(checkOut);
        assertThat(response.guestsCount()).isEqualTo(3);
        assertThat(response.totalPrice()).isEqualTo(900f);
        assertThat(response.status()).isEqualTo(BookingStatus.PENDING);
        assertThat(response.createdAt()).isEqualTo(booking.getCreatedAt());

        // Os quartos foram convertidos pelo RoomMapper (a ordem é preservada)
        assertThat(response.rooms()).hasSize(2);
        assertThat(response.rooms().get(0).room_id()).isEqualTo(100L);
        assertThat(response.rooms().get(1).room_id()).isEqualTo(101L);
        assertThat(response.rooms().get(1).maxCapacity()).isEqualTo(2);
    }
}
