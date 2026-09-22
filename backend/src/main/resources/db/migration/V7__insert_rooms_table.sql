-- Cinco quartos de exemplo para cada hotel existente ao executar esta migration.
-- AVALIABLE corresponde ao nome atual do enum RoomStatus.
INSERT INTO tb_rooms (name, description, room_type, room_status, price, img_url, hotel_id)
SELECT room.name, room.description, room.room_type, 'AVALIABLE', room.price,
       room.img_url, hotel.hotel_id
FROM tb_hotels hotel
         CROSS JOIN (VALUES
                         ('Individual Standard', 'Quarto individual com cama de solteiro, banheiro privativo e mesa de trabalho.', 'SINGLE', 180.00, 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304'),
                         ('Individual Superior', 'Quarto individual com cama de solteiro e espaço extra para descansar.', 'SINGLE', 220.00, 'https://images.unsplash.com/photo-1611892440504-42a792e24d32'),
                         ('Suíte Casal', 'Suíte com cama de casal e banheiro privativo para uma estadia a dois.', 'SUITE', 320.00, 'https://images.unsplash.com/photo-1618773928121-c32242e63f39'),
                         ('Suíte Premium', 'Suíte para duas pessoas com cama de casal e área de estar.', 'SUITE', 450.00, 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b'),
                         ('Quarto Triplo', 'Quarto com uma cama de casal e uma de solteiro para até três pessoas.', 'TRIPLE', 380.00, 'https://images.unsplash.com/photo-1590490360182-c33d57733427')
) AS room(name, description, room_type, price, img_url);
