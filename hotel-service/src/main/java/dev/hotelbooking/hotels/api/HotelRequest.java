package dev.hotelbooking.hotels.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelRequest(
        @NotBlank @Size(max = 140) String name,
        @NotBlank @Size(max = 240) String address) {}
