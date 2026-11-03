package com.maisprati.destinify.backend.support;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Hotel;
import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.domain.enums.Role;
import com.maisprati.destinify.backend.domain.enums.RoomStatus;
import com.maisprati.destinify.backend.domain.enums.RoomType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// FÁBRICA DE DADOS DE TESTE (padrão conhecido como "Object Mother").

public final class TestDataFactory {

    private TestDataFactory() {
    }

    // Usuário comum (Role.CLIENT). O e-mail inclui o id para ficar único: maria1@email.com
    public static User client(Long id) {
        return new User(id, "Maria Cliente", "maria" + id + "@email.com",
                "senhaCriptografada", LocalDate.of(1995, 5, 20), Role.CLIENT);
    }

    // Usuário administrador (Role.ADMIN).
    public static User admin(Long id) {
        return new User(id, "Admin Silva", "admin" + id + "@email.com",
                "senhaCriptografada", LocalDate.of(1985, 1, 10), Role.ADMIN);
    }

    public static Hotel hotel(Long id) {
        return new Hotel(id, "Hotel Central", "Rio de Janeiro", "Rua A, 100",
                "Um hotel de teste", 4.5f, "http://img/hotel.png");
    }

    // Quarto de um hotel. O tipo define a capacidade (SINGLE = 1, SUITE = 2, TRIPLE = 3)
    // e o preço é a diária.
    public static Room room(Long id, Hotel hotel, RoomType type, float price) {
        return new Room(id, "Quarto " + id, "Descrição do quarto", type, price,
                "http://img/room.png", RoomStatus.AVALIABLE, hotel);
    }

    public static Booking booking(Long id, User user, Hotel hotel, List<Room> rooms,
                                  LocalDate checkIn, LocalDate checkOut, BookingStatus status) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setUser(user);
        booking.setHotel(hotel);
        booking.setRooms(new ArrayList<>(rooms));
        booking.setCheckInDate(checkIn);
        booking.setCheckOutDate(checkOut);
        booking.setGuestsCount(1);
        booking.setTotalPrice(300f);
        booking.setStatus(status);
        booking.setCreatedAt(LocalDateTime.now());
        return booking;
    }
}
