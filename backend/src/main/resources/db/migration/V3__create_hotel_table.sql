CREATE TABLE tb_hotels
(
    hotel_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(80) NOT NULL,
    address VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    stars float NOT NULL,
    image_url VARCHAR(255)
)
