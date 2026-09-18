package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingCreate;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingResponse;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingUpdate;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.exceptions.BookingConflictException;
import com.maisprati.destinify.backend.exceptions.BookingNotFoundException;
import com.maisprati.destinify.backend.exceptions.RoomNotFoundException;
import com.maisprati.destinify.backend.repositories.BookingRepository;
import com.maisprati.destinify.backend.repositories.RoomRepository;
import com.maisprati.destinify.backend.utils.BookingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingMapper bookingMapper;

    @Transactional
    public BookingResponse newBooking(BookingCreate bookingCreate, User loggedUser) {

        // Regra 1: check-out tem que ser depois do check-in (senão a estadia teria 0 ou -N noites)
        if (!bookingCreate.checkOutDate().isAfter(bookingCreate.checkInDate())) {
            throw new BookingConflictException("A data de check-out deve ser posterior à data de check-in");
        }

        Room room = roomRepository.findById(bookingCreate.roomId())
                .orElseThrow(() -> new RoomNotFoundException(bookingCreate.roomId()));

        // Regra 2: o quarto tem que pertencer mesmo ao hotel informado.
        if (!room.getHotel().getHotel_id().equals(bookingCreate.hotelId())) {
            throw new BookingConflictException("O quarto informado não pertence ao hotel informado");
        }

        // Regra 3: quantidade de hóspedes não pode passar da capacidade do quarto
        int maxCapacity = room.getRoomType().getMaxCapacity();
        if (bookingCreate.guestsCount() > maxCapacity) {
            throw new BookingConflictException("O quarto selecionado comporta no máximo " + maxCapacity + " hóspede(s)");
        }

        // Regra 4: não pode reservar um quarto que já esta ocupado
        // por outra reserva ativa em algum dia dentro do período pedido.
        List<Booking> overlaps = bookingRepository.findOverlappingBookings(
                room.getRoom_id(),
                bookingCreate.checkInDate(),
                bookingCreate.checkOutDate(),
                BookingStatus.CANCELLED
        );
        if (!overlaps.isEmpty()) {
            throw new BookingConflictException("O quarto já está reservado para o período selecionado.");
        }

        long nights = ChronoUnit.DAYS.between(bookingCreate.checkInDate(), bookingCreate.checkOutDate());

        float totalPrice = room.getPrice() * nights;

        Booking booking = new Booking();
        booking.setUser(loggedUser); // vem do tokem (Controller), não do JSON
        booking.setHotel(room.getHotel());
        booking.setRoom(room);
        booking.setCheckInDate(bookingCreate.checkInDate());
        booking.setCheckOutDate(bookingCreate.checkOutDate());
        booking.setGuestsCount(bookingCreate.guestsCount());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);
        return bookingMapper.bookingResponseMapper(savedBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponse findBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        return bookingMapper.bookingResponseMapper(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> findAll(Pageable pageable) {
        Page<Booking> bookingsPage = bookingRepository.findAll(pageable);
        return bookingsPage.map(bookingMapper::bookingResponseMapper);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> findByUserId(Long userId, Pageable pageable) {
        Page<Booking> bookingsPage = bookingRepository.findByUserId(userId, pageable);
        return bookingsPage.map(bookingMapper::bookingResponseMapper);
    }

    @Transactional
    public BookingResponse updateBooking(Long id, BookingUpdate bookingUpdate) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingConflictException("Não é possível editar uma reserva cancelada.");
        }

        var newCheckIn = bookingUpdate.checkInDate() != null ? bookingUpdate.checkInDate() : booking.getCheckInDate();
        var newCheckOut = bookingUpdate.checkOutDate() != null ? bookingUpdate.checkOutDate() : booking.getCheckOutDate();
        var newGuests = bookingUpdate.guestsCount() != null ? bookingUpdate.guestsCount() : booking.getGuestsCount();

        if (!newCheckOut.isAfter(newCheckIn)) {
            throw new BookingConflictException("A data de check-out deve ser posterior à data de check-in");
        }

        int maxCapacity = booking.getRoom().getRoomType().getMaxCapacity();
        if (newGuests > maxCapacity) {
            throw new BookingConflictException("O quarto selecionado comporta no máximo " + maxCapacity + " hóspede(s)");
        }

        List<Booking> overlaps = bookingRepository.findOverlappingBookingsExcludingBooking(
                booking.getRoom().getRoom_id(),
                newCheckIn,
                newCheckOut,
                BookingStatus.CANCELLED,
                booking.getId()
        );

        if (!overlaps.isEmpty()) {
            throw new BookingConflictException("O quarto já está reservado para o novo período selecionado");
        }

        // Recalcula o preço, já que o número de noites pode ter mudado
        long nights = ChronoUnit.DAYS.between(newCheckIn, newCheckOut);
        booking.setCheckInDate(newCheckIn);
        booking.setCheckOutDate(newCheckOut);
        booking.setGuestsCount(newGuests);
        booking.setTotalPrice(booking.getRoom().getPrice() * nights);

        Booking updated = bookingRepository.save(booking);
        return bookingMapper.bookingResponseMapper(updated);
    }

    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingConflictException("Esta reserva já esta cancelada");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingConflictException("Não é possível cancelar uma reserva já concluída");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        Booking updated = bookingRepository.save(booking);
        return bookingMapper.bookingResponseMapper(updated);
    }

    // Exclusão definitiva do registo do banco (ADMIN)
    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        bookingRepository.delete(booking);
    }
}