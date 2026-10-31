package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.*;
import com.maisprati.destinify.backend.domain.enums.RoomType;
import com.maisprati.destinify.backend.repositories.LocalPromotionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PromotionServiceTest {
    private final LocalDate start = LocalDate.now().plusDays(10);
    private PromotionService service(LocalPromotion... events) {
        var repo = (LocalPromotionRepository) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{LocalPromotionRepository.class}, (proxy, method, args) -> {
                    if (method.getName().startsWith("findByStartDate")) return List.of(events);
                    throw new UnsupportedOperationException(method.getName());
                });
        return new PromotionService(repo, new BigDecimal("10"));
    }
    private List<Room> rooms() {
        Hotel hotel = new Hotel(); hotel.setCity("Salvador");
        var rooms = new ArrayList<Room>();
        for (long id = 1; id <= 3; id++) {
            Room room = new Room(); room.setRoom_id(id); room.setHotel(hotel);
            room.setRoomType(RoomType.TRIPLE); room.setPrice(100f); rooms.add(room);
        }
        return rooms;
    }
    private LocalPromotion event(long id, String city, LocalDate first, LocalDate last, String percent) {
        return new LocalPromotion(id, "Festa " + id, city, first, last, new BigDecimal(percent));
    }
    @Test void eightAdultsReceiveGroupDiscountAndChildStillCounts() {
        var result = service().calculate(rooms(), start, start.plusDays(2), 9, 8, List.of(4));
        assertEquals(new BigDecimal("486.00"), result.totalPrice());
        assertEquals(new BigDecimal("114.00"), result.discountTotal());
        assertEquals("CHILD_UNDER_7", result.promotions().getLast().getCode());
    }
    @Test void nineAdultsReceiveGroupDiscount() {
        var result = service().calculate(rooms(), start, start.plusDays(2), 9, 9, List.of());
        assertEquals(new BigDecimal("540.00"), result.totalPrice());
    }
    @Test void groupAndLocalDiscountsApplySequentially() {
        var result = service(event(1, "Sálvador", start, start.plusDays(2), "10"))
                .calculate(rooms(), start, start.plusDays(2), 8, 8, List.of());
        assertEquals(new BigDecimal("486.00"), result.totalPrice());
        assertEquals(new BigDecimal("114.00"), result.discountTotal());
    }
    @Test void onlyEligibleNightsReceiveLocalDiscountAndCheckoutIsExcluded() {
        var result = service(event(1, "Salvador", start.plusDays(1), start.plusDays(2), "10"))
                .calculate(rooms(), start, start.plusDays(2), 8, 8, List.of());
        assertEquals(new BigDecimal("513.00"), result.totalPrice());
        assertEquals(1, result.promotions().getLast().getNights());
        var checkoutOnly = service(event(1, "Salvador", start.plusDays(2), start.plusDays(3), "10"))
                .calculate(rooms(), start, start.plusDays(2), 9, 9, List.of());
        assertEquals(new BigDecimal("540.00"), checkoutOnly.totalPrice());
    }
    @Test void otherCitiesAndOverlappingFestivalsDoNotStack() {
        var result = service(event(1, "Outra cidade", start, start.plusDays(2), "90"),
                event(2, "Salvador", start, start.plusDays(2), "10"),
                event(3, "Salvador", start, start.plusDays(2), "20"))
                .calculate(rooms(), start, start.plusDays(2), 9, 9, List.of());
        assertEquals(new BigDecimal("432.00"), result.totalPrice());
        assertEquals(2, result.promotions().size());
    }
    @Test void validatesGuestBreakdownAgeAndCapacity() {
        assertThrows(ResponseStatusException.class, () -> service().calculate(rooms(), start, start.plusDays(1), 3, 2, List.of()));
        assertThrows(ResponseStatusException.class, () -> service().calculate(rooms(), start, start.plusDays(1), 3, 2, List.of(18)));
        assertThrows(ResponseStatusException.class, () -> service().calculate(rooms(), start, start.plusDays(1), 10, 9, List.of(1)));
        assertThrows(ResponseStatusException.class, () -> service().calculate(rooms(), start, start, 2, 2, List.of()));
    }
    @Test void sevenYearOldIsOutsideTheUnderSevenBenefit() {
        var result = service().calculate(rooms(), start, start.plusDays(1), 3, 2, List.of(7));
        assertTrue(result.promotions().stream().noneMatch(p -> p.getCode().equals("CHILD_UNDER_7")));
        assertEquals(new BigDecimal("300.00"), result.totalPrice());
    }
    @Test void legacyRequestsInferAdultsAndMoneyUsesTwoDecimals() {
        var result = service().calculate(rooms(), start, start.plusDays(1), 9, null, null);
        assertEquals(9, result.adultsCount());
        assertEquals(new BigDecimal("270.00"), result.totalPrice());
    }

    @Test void fractionalDailyPricesRoundToCentsWithoutChangingTheDiscountBreakdown() {
        var selected = rooms();
        selected.forEach(room -> room.setPrice(100.05f));
        var result = service(event(1, "Salvador", start, start, "10"))
                .calculate(selected, start, start.plusDays(1), 8, 8, List.of());
        assertEquals(new BigDecimal("300.15"), result.basePrice());
        assertEquals(new BigDecimal("243.12"), result.totalPrice());
        assertEquals(result.basePrice().subtract(result.totalPrice()), result.discountTotal());
        assertEquals(result.discountTotal(), result.promotions().stream().map(PromotionDiscount::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }
    @Test void noGroupDiscountBelowEightPeople() {
        for (int adults : List.of(1, 7)) {
            var result = service().calculate(rooms(), start, start.plusDays(1), adults, adults, List.of());
            assertEquals(new BigDecimal("300.00"), result.totalPrice());
            assertTrue(result.promotions().stream().noneMatch(p -> p.getCode().equals("GROUP")));
        }
    }

    @Test void underSevenDiscountAppliesOnceRegardlessOfChildCount() {
        for (var ages : List.of(List.of(0), List.of(6), List.of(2, 4, 6))) {
            var result = service().calculate(rooms(), start, start.plusDays(1), 2 + ages.size(), 2, ages);
            assertEquals(new BigDecimal("270.00"), result.totalPrice());
            assertEquals(1, result.promotions().stream().filter(p -> p.getCode().equals("CHILD_UNDER_7")).count());
            assertEquals(new BigDecimal("30.00"), result.promotions().getLast().getAmount());
        }
    }

    @Test void childDiscountAppliesAfterGroupAndLocalPromotions() {
        var result = service(event(1, "Salvador", start, start.plusDays(1), "10"))
                .calculate(rooms(), start, start.plusDays(1), 8, 7, List.of(6));
        assertEquals(new BigDecimal("218.70"), result.totalPrice());
        assertEquals(new BigDecimal("81.30"), result.discountTotal());
        assertEquals(result.discountTotal(), result.promotions().stream().map(PromotionDiscount::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    @Test void eightPeopleQualifyEvenWithFewerThanEightAdults() {
        var result = service().calculate(rooms(), start, start.plusDays(1), 8, 7, List.of(7));
        assertEquals(new BigDecimal("270.00"), result.totalPrice());
        assertEquals("GROUP", result.promotions().getFirst().getCode());
    }
}
