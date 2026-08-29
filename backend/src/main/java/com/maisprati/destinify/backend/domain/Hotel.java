package com.maisprati.destinify.backend.domain;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "tb_hotels")

public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String city;
    private String address;

    @Column(length = 1000)
    private String description;

    private Double pricePerNight;
    private Integer stars;
    private String imageUrl;

    public Hotel() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String nome) { this.name = nome; }
    public String getCity() {return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getPricePerNight() { return pricePerNight; }
    public void setPricePerNight(Double pricePerNight) { this.pricePerNight = pricePerNight; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @Override
    public boolean equals(Object o) {
        if(o == null || getClass() != o.getClass()) return false;
        Hotel hotel = (Hotel) o;
        return Objects.equals(id, hotel.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }
}
