package com.maisprati.destinify.backend.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Embeddable
public class PromotionDiscount {
    private String code;
    private String name;
    private BigDecimal percentage;
    private BigDecimal amount;
    private long nights;
    public PromotionDiscount() {}
    public PromotionDiscount(String code, String name, BigDecimal percentage, BigDecimal amount, long nights) {
        this.code = code; this.name = name; this.percentage = percentage; this.amount = amount; this.nights = nights;
    }
    public String getCode() { return code; }
    public String getName() { return name; }
    public BigDecimal getPercentage() { return percentage; }
    public BigDecimal getAmount() { return amount; }
    public long getNights() { return nights; }
}
