package dev.hotelbooking.hotels.model;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(name = "uk_room_hotel_number", columnNames = {"hotel_id", "number"}))
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;
    @Column(nullable = false, length = 30)
    private String number;
    @Column(nullable = false)
    private boolean available = true;
    @Column(name = "times_booked", nullable = false)
    private long timesBooked;

    protected Room() {}

    public Room(Hotel hotel, String number, boolean available) {
        this.hotel = hotel;
        this.number = number;
        this.available = available;
    }

    public Long getId() { return id; }
    public Hotel getHotel() { return hotel; }
    public String getNumber() { return number; }
    public boolean isAvailable() { return available; }
    public long getTimesBooked() { return timesBooked; }
    public void setHotel(Hotel hotel) { this.hotel = hotel; }
    public void setNumber(String number) { this.number = number; }
    public void setAvailable(boolean available) { this.available = available; }
    public void setTimesBooked(long timesBooked) { this.timesBooked = timesBooked; }
}
