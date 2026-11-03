package com.maisprati.destinify.backend.domain.dto.BookingDTO;

import com.maisprati.destinify.backend.domain.PromotionDiscount;
import java.math.BigDecimal;
import java.util.List;

public record BookingSummary(long nights, int adultsCount, List<Integer> childrenAges,
                             BigDecimal basePrice, BigDecimal discountTotal, BigDecimal totalPrice,
                             List<PromotionDiscount> promotions, String cancellationPolicy,
                             Long hotelId, String hotelName, java.time.LocalDate checkInDate,
                             java.time.LocalDate checkOutDate, List<Long> roomIds) {}
