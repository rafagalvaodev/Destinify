package com.maisprati.destinify.backend.repositories;

import com.maisprati.destinify.backend.domain.LocalPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface LocalPromotionRepository extends JpaRepository<LocalPromotion, Long> {
    List<LocalPromotion> findByStartDateLessThanAndEndDateGreaterThanEqual(LocalDate end, LocalDate start);
}
