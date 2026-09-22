package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.Booking;
import com.maisprati.destinify.backend.domain.Room;
import com.maisprati.destinify.backend.domain.User;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingCreate;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingResponse;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingUpdate;
import com.maisprati.destinify.backend.domain.enums.BookingStatus;
import com.maisprati.destinify.backend.exceptions.BookingCancellationDeadlineException;
import com.maisprati.destinify.backend.exceptions.BookingConflictException;
import com.maisprati.destinify.backend.exceptions.BookingNotFoundException;
import com.maisprati.destinify.backend.exceptions.RoomNotFoundException;
import com.maisprati.destinify.backend.repositories.BookingRepository;
import com.maisprati.destinify.backend.repositories.RoomRepository;
import com.maisprati.destinify.backend.utils.mappers.BookingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookingMapper bookingMapper;

    @Autowired
    private PaymentService paymentService;
//
//    @Transactional
//    public BookingResponse newBooking(BookingCreate bookingCreate, User loggedUser) {
//
//        // Regra 1: check-out tem que ser depois do check-in (senão a estadia teria 0 ou -N noites)
//        if (!bookingCreate.checkOutDate().isAfter(bookingCreate.checkInDate())) {
//            throw new BookingConflictException("A data de check-out deve ser posterior à data de check-in");
//        }
//
//        List<Long> roomIds = bookingCreate
//                .roomIds()
//                .stream()
//                .distinct()
//                .sorted()
//                .toList();
//
//        List<Room> rooms = new ArrayList<>();
//
//        for (Long roomId : roomIds) {
//            Room room = roomRepository
//                    .findByIdForUpdate(roomId)
//                    .orElseThrow(() -> new RoomNotFoundException(roomId));
//
//            // Regra 2: o quarto tem que pertencer mesmo ao hotel informado.
//            if (!room.getHotel().getHotel_id().equals(bookingCreate.hotelId())) {
//                throw new BookingConflictException("Todos os quartos devem pertence ao hotel informado");
//            }
//
//            // Regra 4: não pode reservar um quarto que já esta ocupado
//            // por outra reserva ativa em algum dia dentro do período pedido.
//            List<Booking> overlaps = bookingRepository.findOverlappingBookings(
//                    roomId,
//                    bookingCreate.checkInDate(),
//                    bookingCreate.checkOutDate(),
//                    BookingStatus.CANCELLED
//            );
//
//            if (!overlaps.isEmpty()) {
//                throw new BookingConflictException(String.format("O quarto %d já está reservado para o período selecionado.", roomId));
//            }
//
//            rooms.add(room);
//
//        }
//
//
//
//        // Regra 3: quantidade de hóspedes não pode passar da capacidade do quarto
//        int maxCapacity = rooms.stream().mapToInt(room ->
//                room.getRoomType()
//                        .getMaxCapacity()).sum();
//
//        if (bookingCreate.guestsCount() > maxCapacity) {
//            throw new BookingConflictException(String.format("O quarto selecionado comporta no máximo %d hospede(s)", maxCapacity ));
//        }
//
//
//
//
//        long nights = ChronoUnit.DAYS.between(
//                bookingCreate.checkInDate(),
//                bookingCreate.checkOutDate());
//
//        float dailyPrice = (float) rooms.stream().mapToDouble(Room::getPrice).sum();
//
//        float totalPrice = dailyPrice * nights;
//
//        Booking booking = new Booking();
//        booking.setUser(loggedUser); // vem do tokem (Controller), não do JSON
//        booking.setHotel(rooms.getFirst().getHotel());
//        booking.setRooms(rooms);
//        booking.setCheckInDate(bookingCreate.checkInDate());
//        booking.setCheckOutDate(bookingCreate.checkOutDate());
//        booking.setGuestsCount(bookingCreate.guestsCount());
//        booking.setTotalPrice(totalPrice);
//        booking.setStatus(BookingStatus.PENDING);
//
//        Booking savedBooking = bookingRepository.save(booking);
//
//        paymentService.createPayment(savedBooking.getId(), loggedUser);
//
//        return bookingMapper.bookingResponseMapper(savedBooking);
//    }

    @Transactional
    public BookingResponse newBooking(
            BookingCreate bookingCreate,
            User loggedUser) {

        if (!bookingCreate.checkOutDate()
                .isAfter(bookingCreate.checkInDate())) {
            throw new BookingConflictException(
                    "A data de check-out deve ser posterior à data de check-in");
        }

        List<Long> roomIds = bookingCreate.roomIds()
                .stream()
                .distinct()
                .sorted()
                .toList();

        List<Room> rooms = new ArrayList<>();

        for (Long roomId : roomIds) {
            Room room = roomRepository.findByIdForUpdate(roomId)
                    .orElseThrow(() -> new RoomNotFoundException(roomId));

            if (!room.getHotel().getHotel_id()
                    .equals(bookingCreate.hotelId())) {
                throw new BookingConflictException(
                        "Todos os quartos devem pertencer ao hotel informado");
            }

            List<Booking> overlaps =
                    bookingRepository.findOverlappingBookings(
                            roomId,
                            bookingCreate.checkInDate(),
                            bookingCreate.checkOutDate(),
                            BookingStatus.CANCELLED
                    );

            if (!overlaps.isEmpty()) {
                throw new BookingConflictException(
                        "O quarto " + roomId +
                                " já está reservado para o período selecionado");
            }

            rooms.add(room);
        }

        int totalCapacity = rooms.stream()
                .mapToInt(room ->
                        room.getRoomType().getMaxCapacity())
                .sum();

        if (bookingCreate.guestsCount() > totalCapacity) {
            throw new BookingConflictException(
                    "Os quartos selecionados comportam no máximo "
                            + totalCapacity + " hóspede(s)");
        }

        long nights = ChronoUnit.DAYS.between(
                bookingCreate.checkInDate(),
                bookingCreate.checkOutDate());

        float dailyPrice = (float) rooms.stream()
                .mapToDouble(Room::getPrice)
                .sum();

        float totalPrice = dailyPrice * nights;

        Booking booking = new Booking();
        booking.setUser(loggedUser);
        booking.setHotel(rooms.getFirst().getHotel());
        booking.setRooms(rooms);
        booking.setCheckInDate(bookingCreate.checkInDate());
        booking.setCheckOutDate(bookingCreate.checkOutDate());
        booking.setGuestsCount(bookingCreate.guestsCount());
        booking.setTotalPrice(totalPrice);
        booking.setStatus(BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);

        paymentService.createPayment(savedBooking.getId(), loggedUser);

        return bookingMapper.bookingResponseMapper(savedBooking);
    }

    @Transactional(readOnly = true)
    public BookingResponse findBookingById(Long id, User loggedUser) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (!booking.getUser().getId().equals(loggedUser.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This reservation belongs to another user");
        }
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
    public BookingResponse updateBooking(User loggedUser, Long id, BookingUpdate bookingUpdate) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (!booking.getUser().getId().equals(loggedUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This reservation belongs to another user");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BookingConflictException("Só é possível editar reservas pendentes.");
        }



        var newCheckIn = bookingUpdate.checkInDate() != null ? bookingUpdate.checkInDate() : booking.getCheckInDate();
        var newCheckOut = bookingUpdate.checkOutDate() != null ? bookingUpdate.checkOutDate() : booking.getCheckOutDate();
        var newGuests = bookingUpdate.guestsCount() != null ? bookingUpdate.guestsCount() : booking.getGuestsCount();

        if (!newCheckOut.isAfter(newCheckIn)) {
            throw new BookingConflictException("A data de check-out deve ser posterior à data de check-in");
        }

        List<Long> roomIds = booking.getRooms()
                .stream()
                .map(Room::getRoom_id)
                .sorted()
                .toList();

        List<Room> lockedRooms = new ArrayList<>();

        for (Long roomId : roomIds) {
            Room room = roomRepository.findByIdForUpdate(roomId)
                    .orElseThrow(() -> new RoomNotFoundException(roomId));

            List<Booking> overlaps = bookingRepository.findOverlappingBookingsExcludingBooking(
                    roomId,
                    newCheckIn,
                    newCheckOut,
                    BookingStatus.CANCELLED,
                    booking.getId()
            );


            if (!overlaps.isEmpty()) {
                throw new BookingConflictException(String.format("O quarto %d já está reservado para o novo período selecionado", roomId));
            }

            lockedRooms.add(room);

        }




        int maxCapacity = lockedRooms.stream().mapToInt(room -> room.getRoomType().getMaxCapacity()).sum();

        if (newGuests > maxCapacity) {
            throw new BookingConflictException(String.format("O quarto selecionado comporta no máximo %d hospede(s)" ,maxCapacity));
        }




        // Recalcula o preço, já que o número de noites pode ter mudado
        long nights = ChronoUnit.DAYS.between(newCheckIn, newCheckOut);


        float dailyPrice = (float) lockedRooms.stream().mapToDouble(Room::getPrice).sum();

        booking.setCheckInDate(newCheckIn);
        booking.setCheckOutDate(newCheckOut);
        booking.setGuestsCount(newGuests);
        booking.setTotalPrice(dailyPrice * nights);

        paymentService.updatePendingAmount(booking);

        Booking updated = bookingRepository.save(booking);
        return bookingMapper.bookingResponseMapper(updated);
    }

    @Transactional
    public BookingResponse cancelBooking(User loggedUser, Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        LocalDateTime checkInStart = booking.getCheckInDate().atTime(14, 0);
        LocalDateTime limite = LocalDateTime.now().plusHours(72);

        if (!booking.getUser().getId().equals(loggedUser.getId())){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This reservation belongs to another user");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BookingConflictException("Esta reserva já esta cancelada");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BookingConflictException("Não é possível cancelar uma reserva já concluída");
        }

        if (!checkInStart.isAfter(limite)){
            throw new BookingCancellationDeadlineException();
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