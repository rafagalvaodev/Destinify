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

import java.time.LocalDateTime;
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
    @Autowired
    private PromotionService promotionService;

    @Transactional(readOnly = true)
    public com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingSummary summary(BookingCreate request) {
        var rooms = loadRooms(request, false);
        return promotionService.calculate(rooms, request.checkInDate(), request.checkOutDate(),
                request.guestsCount(), request.adultsCount(), request.childrenAges());
    }

    private List<Room> loadRooms(BookingCreate request, boolean lock) {
        if (request.checkInDate().isBefore(java.time.LocalDate.now()) || !request.checkOutDate().isAfter(request.checkInDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe datas válidas: saída após chegada.");
        var rooms = new ArrayList<Room>();
        for (Long id : request.roomIds().stream().distinct().sorted().toList()) {
            Room room = (lock ? roomRepository.findByIdForUpdate(id) : roomRepository.findById(id))
                    .orElseThrow(() -> new RoomNotFoundException(id));
            if (!room.getHotel().getHotel_id().equals(request.hotelId()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Os quartos devem pertencer ao mesmo hotel.");
            if (room.getRoomStatus() != com.maisprati.destinify.backend.domain.enums.RoomStatus.AVALIABLE)
                throw new BookingConflictException("Quarto indisponível para reserva.");
            if (!bookingRepository.findOverlappingBookings(id, request.checkInDate(), request.checkOutDate(), BookingStatus.CANCELLED).isEmpty())
                throw new BookingConflictException("O quarto " + id + " já está reservado para o período selecionado.");
            rooms.add(room);
        }
        return rooms;
    }

    private void applyPrice(Booking booking, com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingSummary price) {
        booking.setAdultsCount(price.adultsCount());
        booking.setChildrenAges(price.childrenAges());
        booking.setBasePrice(price.basePrice());
        booking.setDiscountTotal(price.discountTotal());
        booking.setTotalPrice(price.totalPrice());
        booking.setPromotions(price.promotions());
    }

    @Transactional
    public BookingResponse newBooking(
            BookingCreate bookingCreate,
            User loggedUser) {

        List<Room> rooms = loadRooms(bookingCreate, true);
        var price = promotionService.calculate(rooms, bookingCreate.checkInDate(), bookingCreate.checkOutDate(),
                bookingCreate.guestsCount(), bookingCreate.adultsCount(), bookingCreate.childrenAges());

        Booking booking = new Booking();
        booking.setUser(loggedUser);
        booking.setHotel(rooms.getFirst().getHotel());
        booking.setRooms(rooms);
        booking.setCheckInDate(bookingCreate.checkInDate());
        booking.setCheckOutDate(bookingCreate.checkOutDate());
        booking.setGuestsCount(bookingCreate.guestsCount());
        applyPrice(booking, price);
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




        var children = bookingUpdate.childrenAges() == null ? booking.getChildrenAges() : bookingUpdate.childrenAges();
        int adults = bookingUpdate.adultsCount() != null ? bookingUpdate.adultsCount() : newGuests - children.size();
        var price = promotionService.calculate(lockedRooms, newCheckIn, newCheckOut, newGuests, adults, children);
        booking.setCheckInDate(newCheckIn);
        booking.setCheckOutDate(newCheckOut);
        booking.setGuestsCount(newGuests);
        applyPrice(booking, price);

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