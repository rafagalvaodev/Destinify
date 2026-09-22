CREATE TABLE tb_payments (
     id BIGSERIAL PRIMARY KEY,
     booking_id BIGINT NOT NULL,
     amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
     payment_status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
     processed_at TIMESTAMP,

     CONSTRAINT fk_payments_bookings
         FOREIGN KEY (booking_id) REFERENCES tb_bookings (id),

     CONSTRAINT ck_payments_status
         CHECK (payment_status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE UNIQUE INDEX uq_payments_pending_booking
    ON tb_payments (booking_id)
    WHERE payment_status = 'PENDING';