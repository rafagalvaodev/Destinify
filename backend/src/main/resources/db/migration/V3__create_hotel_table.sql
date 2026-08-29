CREATE TABLE tb_hotels
(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(80) NOT NULL,
    address VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    price_per_night NUMERIC(10, 2) NOT NULL,
    stars INTEGER NOT NULL,
    image_url VARCHAR(255)
)