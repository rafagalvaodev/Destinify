package com.maisprati.destinify.backend.servicies;

import com.maisprati.destinify.backend.domain.*;
import com.maisprati.destinify.backend.domain.dto.BookingDTO.BookingSummary;
import com.maisprati.destinify.backend.repositories.LocalPromotionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class PromotionService {
    private final LocalPromotionRepository repository;
    private final BigDecimal groupPercentage;
    public PromotionService(LocalPromotionRepository repository,
                            @Value("${destinify.promotions.group-percentage:10}") BigDecimal groupPercentage) {
        if (groupPercentage.signum() < 0 || groupPercentage.compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("Percentual de grupo deve estar entre 0 e 100.");
        this.repository = repository;
        this.groupPercentage = groupPercentage;
    }

    public BookingSummary calculate(List<Room> rooms, LocalDate start, LocalDate end,
                                    int guests, Integer adults, List<Integer> ages) {
        if (ages != null && ages.stream().anyMatch(Objects::isNull))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a idade de cada criança.");
        var children = ages == null ? List.<Integer>of() : List.copyOf(ages);
        int adultCount = adults == null ? guests - children.size() : adults;
        if (adultCount < 1 || guests > 60 || adultCount + children.size() != guests
                || children.stream().anyMatch(age -> age < 0 || age > 17)
                || start == null || end == null || !end.isAfter(start) || start.isBefore(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confira datas, adultos e idades das crianças (0 a 17 anos). O total deve corresponder aos hóspedes.");
        if (rooms.isEmpty() || rooms.stream().mapToInt(r -> r.getRoomType().getMaxCapacity()).sum() < guests)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Os quartos não comportam todos os hóspedes, incluindo crianças.");
        if (rooms.stream().anyMatch(r -> r.getPrice() == null || !Float.isFinite(r.getPrice()) || r.getPrice() <= 0))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quartos devem possuir diárias positivas válidas.");
        long nights = ChronoUnit.DAYS.between(start, end);
        if (nights > 366) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O período máximo é de 366 noites.");
        BigDecimal daily = rooms.stream().map(r -> new BigDecimal(r.getPrice().toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal base = money(daily.multiply(BigDecimal.valueOf(nights)));
        var discounts = new ArrayList<PromotionDiscount>();
        BigDecimal groupRate = guests >= 8 ? groupPercentage.movePointLeft(2) : BigDecimal.ZERO;
        BigDecimal groupDiscount = money(base.multiply(groupRate));
        if (groupRate.signum() > 0) discounts.add(new PromotionDiscount("GROUP", "Reserva com 8 ou mais pessoas", groupPercentage, groupDiscount, nights));
        var candidates = repository.findByStartDateLessThanAndEndDateGreaterThanEqual(end, start).stream()
                .filter(p -> normalized(p.getCity()).equals(normalized(rooms.getFirst().getHotel().getCity())))
                .sorted(Comparator.comparing(LocalPromotion::getPercentage).reversed().thenComparing(LocalPromotion::getId)).toList();
        var eligibleNights = new LinkedHashMap<LocalPromotion, Long>();
        for (LocalDate night = start; night.isBefore(end); night = night.plusDays(1)) {
            for (var promotion : candidates) {
                if (!night.isBefore(promotion.getStartDate()) && !night.isAfter(promotion.getEndDate())) {
                    eligibleNights.merge(promotion, 1L, Long::sum);
                    break; // Apenas a maior promoção local por noite.
                }
            }
        }
        BigDecimal discount = groupDiscount;
        for (var entry : eligibleNights.entrySet()) {
            var promotion = entry.getKey();
            BigDecimal amount = money(daily.multiply(BigDecimal.ONE.subtract(groupRate))
                    .multiply(BigDecimal.valueOf(entry.getValue())).multiply(promotion.getPercentage().movePointLeft(2)));
            amount = amount.min(base.subtract(discount).max(BigDecimal.ZERO));
            discounts.add(new PromotionDiscount("LOCAL_" + promotion.getId(), promotion.getName(), promotion.getPercentage(), amount, entry.getValue()));
            discount = discount.add(amount);
        }
        if (children.stream().anyMatch(age -> age < 7)) {
            BigDecimal childDiscount = money(base.subtract(discount).max(BigDecimal.ZERO).multiply(new BigDecimal("0.10")));
            discounts.add(new PromotionDiscount("CHILD_UNDER_7", "Criança menor de 7 anos — 10% por reserva",
                    new BigDecimal("10"), childDiscount, nights));
            discount = discount.add(childDiscount);
        }
        return new BookingSummary(nights, adultCount, children, base, discount, money(base.subtract(discount).max(BigDecimal.ZERO)),
                discounts, "Cancelamento permitido com mais de 72 horas de antecedência do check-in às 14h.",
                rooms.getFirst().getHotel().getHotel_id(), rooms.getFirst().getHotel().getName(), start, end,
                rooms.stream().map(Room::getRoom_id).toList());
    }
    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private static String normalized(String value) {
        return Normalizer.normalize(value.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
