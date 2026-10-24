package dev.hotelbooking.bookings.service;

import dev.hotelbooking.bookings.api.BookingRequest;
import dev.hotelbooking.bookings.model.Booking;
import dev.hotelbooking.bookings.model.BookingStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingOrchestratorTest {
    @Mock BookingPersistence persistence;
    @Mock HotelClient hotelClient;
    @InjectMocks BookingOrchestrator orchestrator;

    @Test
    void successfulReservationConfirmsPendingBooking() {
        LocalDate in = LocalDate.now().plusDays(10);
        LocalDate out = in.plusDays(2);
        Booking pending = new Booking(7L, "guest", null, in, out, "request-1");
        BookingRequest request = new BookingRequest(null, true, in, out);
        when(persistence.createPending("guest", null, in, out, "request-1")).thenReturn(pending);
        when(hotelClient.recommend(in, out)).thenReturn(List.of(
                new HotelClient.RoomCandidate(15L, 2L, "Riverlight", "14", true, 0)));
        when(hotelClient.reserve(15L, pending.getId(), "request-1", in, out))
                .thenReturn(new HotelClient.RoomCandidate(15L, 2L, "Riverlight", "14", true, 1));
        when(persistence.confirm(pending.getId(), 15L)).thenAnswer(call -> {
            pending.setRoomId(15L);
            pending.setStatus(BookingStatus.CONFIRMED);
            return pending;
        });

        Booking result = orchestrator.create("guest", request, "request-1");

        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        assertEquals(15L, result.getRoomId());
        verify(persistence, never()).cancel(anyString(), anyString(), anyBoolean());
        verify(hotelClient, never()).release(anyLong(), anyString(), anyString());
    }

    @Test
    void remoteFailureCancelsBookingAndReleasesPossiblyCreatedHold() {
        LocalDate in = LocalDate.now().plusDays(10);
        LocalDate out = in.plusDays(1);
        Booking pending = new Booking(7L, "guest", 15L, in, out, "request-2");
        BookingRequest request = new BookingRequest(15L, false, in, out);
        when(persistence.createPending("guest", 15L, in, out, "request-2")).thenReturn(pending);
        when(hotelClient.reserve(15L, pending.getId(), "request-2", in, out))
                .thenThrow(new HotelClient.HotelServiceUnavailableException("timeout", new RuntimeException("timeout")));
        when(persistence.cancel(pending.getId(), "guest", true)).thenAnswer(call -> {
            pending.setStatus(BookingStatus.CANCELLED);
            return pending;
        });

        assertThrows(HotelClient.HotelServiceUnavailableException.class,
                () -> orchestrator.create("guest", request, "request-2"));

        assertEquals(BookingStatus.CANCELLED, pending.getStatus());
        verify(hotelClient).release(15L, pending.getId(), "request-2");
    }

    @Test
    void repeatedIdempotencyKeyReturnsExistingBookingWithoutCallingHotel() {
        LocalDate in = LocalDate.now().plusDays(10);
        LocalDate out = in.plusDays(2);
        Booking existing = new Booking(7L, "guest", 15L, in, out, "request-3");
        existing.setStatus(BookingStatus.CONFIRMED);
        BookingRequest request = new BookingRequest(null, true, in, out);
        when(persistence.createPending("guest", null, in, out, "request-3"))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(persistence.byRequestId("request-3")).thenReturn(existing);

        Booking result = orchestrator.create("guest", request, "request-3");

        assertSame(existing, result);
        assertEquals(BookingStatus.CONFIRMED, result.getStatus());
        verifyNoInteractions(hotelClient);
    }
}
