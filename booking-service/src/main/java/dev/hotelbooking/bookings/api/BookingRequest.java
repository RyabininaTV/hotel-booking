package dev.hotelbooking.bookings.api;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record BookingRequest(
        Long roomId,
        boolean autoSelect,
        @NotNull @FutureOrPresent LocalDate checkIn,
        @NotNull LocalDate checkOut) {
    @AssertTrue(message = "roomId is required unless autoSelect is true")
    public boolean isSelectionValid() { return autoSelect || roomId != null; }

    @AssertTrue(message = "checkOut must be after checkIn")
    public boolean isDateRangeValid() { return checkIn == null || checkOut == null || checkOut.isAfter(checkIn); }
}
