package dev.hotelbooking.hotels.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record AvailabilityRequest(
        @NotBlank String bookingId,
        @NotBlank String requestId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut) {}
