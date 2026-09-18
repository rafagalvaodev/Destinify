CREATE TABLE tb_bookings
(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    hotel_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    guests_count INT NOT NULL,
    total_price FLOAT NOT NULL,
    status VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    --FK para tb_users: se o usuário for exluído, suas reservas somem junto (ON DELETE CASCADE)
    CONSTRAINT fk_bookings_users
        FOREIGN KEY (user_id)
        REFERENCES tb_users (id)
        ON DELETE CASCADE,

    -- FK para tb_hotels
    CONSTRAINT fk_bookings_hotels
        FOREIGN KEY (hotel_id)
        REFERENCES tb_hotels (hotel_id)
        ON DELETE CASCADE,

    -- FK para tb_rooms
    CONSTRAINT fk_bookings_rooms
        FOREIGN KEY (room_id)
        REFERENCES tb_rooms (room_id)
        ON DELETE CASCADE
);