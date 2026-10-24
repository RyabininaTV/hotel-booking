package dev.hotelbooking.bookings.service;

import dev.hotelbooking.bookings.api.BookingRequest;
import dev.hotelbooking.bookings.model.Booking;
import dev.hotelbooking.bookings.model.BookingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookingOrchestrator {
    private static final Logger log = LoggerFactory.getLogger(BookingOrchestrator.class);
    private final BookingPersistence persistence;
    private final HotelClient hotelClient;

    public BookingOrchestrator(BookingPersistence persistence, HotelClient hotelClient) {
        this.persistence = persistence;
        this.hotelClient = hotelClient;
    }

    public Booking create(String username, BookingRequest request, String requestId) {
        Booking pending;
        try {
            pending = persistence.createPending(username, request.autoSelect() ? null : request.roomId(),
                    request.checkIn(), request.checkOut(), requestId);
        } catch (DataIntegrityViolationException duplicate) {
            Booking existing = persistence.byRequestId(requestId);
            boolean sameRequest = existing.getUsername().equals(username)
                    && existing.getStartDate().equals(request.checkIn())
                    && existing.getEndDate().equals(request.checkOut())
                    && (request.autoSelect() || existing.getRoomId() == null || existing.getRoomId().equals(request.roomId()));
            if (!sameRequest) throw new IllegalStateException("Idempotency key was already used for a different booking request");
            log.info("booking_idempotent_replay bookingId={} status={}", existing.getId(), existing.getStatus());
            return existing;
        }

        log.info("booking_pending bookingId={} requestId={} username={}", pending.getId(), requestId, username);
        List<Long> candidates;
        try {
            candidates = request.autoSelect()
                    ? hotelClient.recommend(request.checkIn(), request.checkOut()).stream().map(HotelClient.RoomCandidate::id).toList()
                    : List.of(request.roomId());
        } catch (RuntimeException error) {
            compensate(pending, null, error);
            throw error;
        }
        if (candidates.isEmpty()) {
            compensate(pending, null, new NoAvailableRoomException("No rooms are available for the requested dates"));
            throw new NoAvailableRoomException("No rooms are available for the requested dates");
        }

        List<Long> conflicts = new ArrayList<>();
        for (Long roomId : candidates) {
            try {
                persistence.assignPendingRoom(pending.getId(), roomId);
                hotelClient.reserve(roomId, pending.getId(), requestId, request.checkIn(), request.checkOut());
                Booking confirmed = persistence.confirm(pending.getId(), roomId);
                log.info("booking_confirmed bookingId={} roomId={} requestId={}", pending.getId(), roomId, requestId);
                return confirmed;
            } catch (HotelClient.RoomUnavailableException conflict) {
                conflicts.add(roomId);
                log.info("booking_room_conflict bookingId={} roomId={}", pending.getId(), roomId);
                if (!request.autoSelect()) break;
            } catch (RuntimeException error) {
                compensate(pending, roomId, error);
                throw error;
            }
        }
        NoAvailableRoomException unavailable = new NoAvailableRoomException(
                conflicts.isEmpty() ? "No rooms are available for the requested dates" : "All candidate rooms were reserved concurrently");
        compensate(pending, null, unavailable);
        throw unavailable;
    }

    public Booking cancel(String id, String username, boolean administrator) {
        Booking before = persistence.getOwned(id, username, administrator);
        Booking cancelled = persistence.cancel(id, username, administrator);
        if (before.getRoomId() != null) {
            try {
                hotelClient.release(before.getRoomId(), before.getId(), before.getRequestId());
                persistence.markRoomReleased(before.getId());
                log.info("booking_cancelled_and_released bookingId={} roomId={}", id, before.getRoomId());
            } catch (RuntimeException error) {
                log.error("booking_cancel_release_failed bookingId={} roomId={} reason={}",
                        id, before.getRoomId(), error.getMessage());
                throw new HotelClient.HotelServiceUnavailableException(
                        "Booking was cancelled locally but room release needs retry", error);
            }
        }
        return cancelled;
    }

    private void compensate(Booking pending, Long roomId, RuntimeException cause) {
        try {
            persistence.cancel(pending.getId(), pending.getUsername(), true);
            log.warn("booking_compensated bookingId={} reason={}", pending.getId(), cause.getMessage());
        } catch (RuntimeException persistenceError) {
            log.error("booking_compensation_failed bookingId={} reason={}", pending.getId(), persistenceError.getMessage());
        }
        if (roomId != null) {
            try {
                hotelClient.release(roomId, pending.getId(), pending.getRequestId());
                persistence.markRoomReleased(pending.getId());
                log.info("room_released_after_compensation bookingId={} roomId={}", pending.getId(), roomId);
            } catch (RuntimeException releaseError) {
                log.error("room_release_failed_after_compensation bookingId={} roomId={} reason={}",
                        pending.getId(), roomId, releaseError.getMessage());
            }
        }
    }

    public static class NoAvailableRoomException extends RuntimeException {
        public NoAvailableRoomException(String message) { super(message); }
    }
}
