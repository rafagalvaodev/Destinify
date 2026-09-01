package com.maisprati.destinify.backend.domain;

import com.maisprati.destinify.backend.domain.enums.RoomType;
import jakarta.persistence.*;

@Entity
@Table(name = "tb_rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long room_id;

    private String name;

    private String description;

    private RoomType roomType;

    private Float price;

    private String imgUrl;

    @ManyToOne
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    public Room() {
    }

    public Room(Long room_id, String name, String description, RoomType roomType, Float price, String imgUrl) {
        this.room_id = room_id;
        this.name = name;
        this.description = description;
        this.roomType = roomType;
        this.price = price;
        this.imgUrl = imgUrl;
    }

    public Long getRoom_id() {
        return room_id;
    }

    public void setRoom_id(Long room_id) {
        this.room_id = room_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }
}
