package dev.hotelbooking.hotels.api;

public record RoomResponse(
        Long id, Long hotelId, String hotelName, String number, boolean available, long timesBooked) {}
