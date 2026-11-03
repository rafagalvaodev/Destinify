package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.*;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.*;
import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import com.maisprati.destinify.backend.repositories.*;
import com.maisprati.destinify.backend.utils.mappers.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=${DESTINIFY_TEST_DATABASE_URL}",
        "spring.datasource.username=${DESTINIFY_TEST_DATABASE_USER:jalapc}",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.show-sql=false" })
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PromotionService.class, BookingService.class, PaymentService.class,
        BookingMapper.class, RoomMapper.class, PaymentMapper.class})
@EnabledIfEnvironmentVariable(named = "DESTINIFY_TEST_DATABASE_URL", matches = ".+")
class PromotionPersistenceTest {
    @Autowired BookingService service;
    @Autowired HotelRepository hotels;
    @Autowired RoomRepository rooms;
    @Autowired BookingRepository bookings;
    @Autowired PaymentRepository payments;
    @Autowired UserRepository users;
    @Autowired EntityManager em;

    @Test void summaryConfirmationUpdateAndPaymentShareTheSamePrice() {
        var hotel = hotels.findAll().stream().filter(h -> h.getCity().equals("Salvador")).findFirst().orElseThrow();
        var selected = rooms.findRoomsByHotelId(hotel.getHotel_id(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        var start = LocalDate.of(2027, 6, 21);
        var request = new BookingCreate(hotel.getHotel_id(), selected.stream().map(Room::getRoom_id).toList(), start, start.plusDays(2), 8, 8, List.of());
        long before = bookings.count();
        var summary = service.summary(request);
        assertEquals(before, bookings.count());
        assertEquals(new BigDecimal("2511.00"), summary.totalPrice());
        var user = users.findAll().getFirst();
        var created = service.newBooking(request, user);
        assertEquals(summary.totalPrice(), created.totalPrice());
        em.flush(); em.clear();
        var saved = bookings.findById(created.id()).orElseThrow();
        assertEquals(2, saved.getPromotions().size());
        assertEquals(summary.basePrice(), saved.getBasePrice());
        assertEquals(summary.discountTotal(), saved.getDiscountTotal());
        assertEquals(summary.totalPrice(), payments.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(created.id(), PaymentStatus.PENDING).orElseThrow().getAmount());
        assertThrows(com.maisprati.destinify.backend.exceptions.BookingConflictException.class, () -> service.summary(request));
    }

    @Test void childAgeAndDiscountSnapshotSurviveReloadAndEditing() {
        var hotel = hotels.findAll().stream().filter(h -> h.getCity().equals("Salvador")).findFirst().orElseThrow();
        var selected = rooms.findRoomsByHotelId(hotel.getHotel_id(), org.springframework.data.domain.Pageable.unpaged()).getContent();
        var start = LocalDate.of(2027, 6, 21);
        var user = users.findAll().getFirst();
        var created = service.newBooking(new BookingCreate(hotel.getHotel_id(), selected.stream().map(Room::getRoom_id).toList(), start, start.plusDays(2), 9, 8, List.of(4)), user);
        em.flush(); em.clear();
        var saved = bookings.findById(created.id()).orElseThrow();
        assertEquals(List.of(4), saved.getChildrenAges());
        assertEquals(8, saved.getAdultsCount());
        var updated = service.updateBooking(user, created.id(), new BookingUpdate(null, start.plusDays(1), 9, null, null));
        assertEquals(new BigDecimal("1129.95"), updated.totalPrice());
        em.flush(); em.clear();
        assertEquals(updated.totalPrice(), payments.findFirstByBookingIdAndPaymentStatusOrderByCreatedAtDesc(created.id(), PaymentStatus.PENDING).orElseThrow().getAmount());
    }
}
