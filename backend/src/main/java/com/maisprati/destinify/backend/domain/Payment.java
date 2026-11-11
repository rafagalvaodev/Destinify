package com.maisprati.destinify.backend.domain;

import com.maisprati.destinify.backend.domain.enums.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    // Dados do checkout criado no PagBank (migração V11). Todos Opcionais: um pagamento
    @Column(name = "provider_checkout_id", length = 60)
    private String providerCheckoutId;  // ex.: CHEC_XXXX

    @Column(name = "checkout_url", length = 500)
    private String checkoutUrl;         // link da página de pagamento do PagBank

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;    // até quando o checkout vale

    public Payment() {

    }

    public Payment(Long id, Booking booking, BigDecimal amount, PaymentStatus paymentStatus, LocalDateTime createdAt, LocalDateTime processedAt) {
        this.id = id;
        this.booking = booking;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getProviderCheckoutId() {
        return providerCheckoutId;
    }

    public void setProviderCheckoutId(String providerCheckoutId) {
        this.providerCheckoutId = providerCheckoutId;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
