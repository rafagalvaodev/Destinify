CREATE TABLE tb_booking_rooms
(
    booking_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,

    PRIMARY KEY (booking_id, room_id),

    CONSTRAINT fk_booking_rooms_booking
        FOREIGN KEY (booking_id)
            REFERENCES tb_bookings (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_booking_rooms_room
        FOREIGN KEY (room_id)
            REFERENCES tb_rooms (room_id)
            ON DELETE CASCADE
);

INSERT INTO tb_booking_rooms (booking_id, room_id)
SELECT id, room_id
FROM tb_bookings;

ALTER TABLE tb_bookings
DROP CONSTRAINT fk_bookings_rooms;

ALTER TABLE tb_bookings
DROP COLUMN room_id;