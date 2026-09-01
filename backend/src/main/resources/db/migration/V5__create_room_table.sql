CREATE TABLE tb_rooms
(
    room_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    room_type VARCHAR(15) NOT NULL,
    price FLOAT NOT NULL,
    img_url VARCHAR(255),
    hotel_id BIGINT NOT NULL,

    CONSTRAINT fk_rooms_hotels
        FOREIGN KEY (hotel_id)
        REFERENCES tb_hotels (hotel_id)
        ON DELETE CASCADE
);
