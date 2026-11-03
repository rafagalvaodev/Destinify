ALTER TABLE tb_bookings ALTER COLUMN total_price TYPE NUMERIC(12,2) USING round(total_price::numeric, 2);
ALTER TABLE tb_bookings ADD COLUMN base_price NUMERIC(12,2);
ALTER TABLE tb_bookings ADD COLUMN discount_total NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE tb_bookings ADD COLUMN adults_count INTEGER;
UPDATE tb_bookings SET base_price = total_price, adults_count = guests_count;
ALTER TABLE tb_bookings ALTER COLUMN base_price SET NOT NULL;
ALTER TABLE tb_bookings ALTER COLUMN adults_count SET NOT NULL;

CREATE TABLE tb_booking_children (
    booking_id BIGINT NOT NULL REFERENCES tb_bookings(id) ON DELETE CASCADE,
    child_index INTEGER NOT NULL,
    age INTEGER NOT NULL CHECK (age BETWEEN 0 AND 17),
    PRIMARY KEY (booking_id, child_index)
);
CREATE TABLE tb_booking_discounts (
    booking_id BIGINT NOT NULL REFERENCES tb_bookings(id) ON DELETE CASCADE,
    discount_index INTEGER NOT NULL,
    code VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    percentage NUMERIC(5,2) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    nights BIGINT NOT NULL,
    PRIMARY KEY (booking_id, discount_index)
);
CREATE TABLE tb_local_promotions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    percentage NUMERIC(5,2) NOT NULL CHECK (percentage > 0 AND percentage <= 100),
    CHECK (end_date >= start_date)
);
-- Calendário demonstrativo para validar a regra; não representa a programação oficial da cidade.
INSERT INTO tb_local_promotions(name, city, start_date, end_date, percentage)
VALUES ('São João — calendário de demonstração', 'Salvador', '2027-06-20', '2027-06-26', 10);
