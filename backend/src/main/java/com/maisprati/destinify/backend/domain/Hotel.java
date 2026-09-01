package com.maisprati.destinify.backend.domain;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tb_hotels")

public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hotel_id;

    private String name;
    private String city;
    private String address;

    @Column(length = 1000)
    private String description;

    private Float stars;
    private String imageUrl;

    @OneToMany(mappedBy = "hotel",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<Room> rooms = new ArrayList<>();

    public Hotel() {}

    public Hotel(Long hotel_id, String name, String city, String address, String description, Float stars, String imageUrl) {
        this.hotel_id = hotel_id;
        this.name = name;
        this.city = city;
        this.address = address;
        this.description = description;
        this.stars = stars;
        this.imageUrl = imageUrl;
    }

    public Long getHotel_id() { return hotel_id; }
    public void setHotel_id(Long hotel_id) { this.hotel_id = hotel_id; }
    public String getName() { return name; }
    public void setName(String nome) { this.name = nome; }
    public String getCity() {return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Float getStars() { return stars; }
    public void setStars(Float stars) { this.stars = stars; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<Room> getRooms() {
        return rooms;
    }

    public void setRooms(List<Room> rooms) {
        this.rooms = rooms;
    }

    @Override
    public boolean equals(Object o) {
        if(o == null || getClass() != o.getClass()) return false;
        Hotel hotel = (Hotel) o;
        return Objects.equals(hotel_id, hotel.hotel_id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(hotel_id); }
}
