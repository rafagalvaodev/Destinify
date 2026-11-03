package com.maisprati.destinify.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "tb_local_promotions")
public class LocalPromotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String city;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal percentage;
    public LocalPromotion() {}
    public LocalPromotion(Long id, String name, String city, LocalDate startDate, LocalDate endDate, BigDecimal percentage) {
        this.id = id; this.name = name; this.city = city;
        this.startDate = startDate; this.endDate = endDate; this.percentage = percentage;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BigDecimal getPercentage() { return percentage; }
}
