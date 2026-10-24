package dev.hotelbooking.bookings.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsInvalidDateRangeAndMissingExplicitRoom() {
        LocalDate checkIn = LocalDate.now().plusDays(4);
        BookingRequest request = new BookingRequest(null, false, checkIn, checkIn);

        var errors = validator.validate(request);

        assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("dateRangeValid")));
        assertTrue(errors.stream().anyMatch(error -> error.getPropertyPath().toString().equals("selectionValid")));
    }

    @Test
    void autoSelectionDoesNotRequireRoomId() {
        LocalDate checkIn = LocalDate.now().plusDays(4);
        BookingRequest request = new BookingRequest(null, true, checkIn, checkIn.plusDays(1));

        assertTrue(validator.validate(request).isEmpty());
    }
}
