package dev.hotelbooking.bookings.service;

import dev.hotelbooking.bookings.model.Booking;
import dev.hotelbooking.bookings.model.BookingStatus;
import dev.hotelbooking.bookings.model.User;
import dev.hotelbooking.bookings.repository.BookingRepository;
import dev.hotelbooking.bookings.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingPersistence {
    private final BookingRepository bookings;
    private final UserRepository users;

    public BookingPersistence(BookingRepository bookings, UserRepository users) {
        this.bookings = bookings;
        this.users = users;
    }

    @Transactional
    public Booking createPending(String username, Long roomId, java.time.LocalDate checkIn,
                                 java.time.LocalDate checkOut, String requestId) {
        User user = users.findByUsername(username).orElseThrow(() -> new EntityNotFoundException("User not found"));
        try {
            return bookings.saveAndFlush(new Booking(user.getId(), username, roomId, checkIn, checkOut, requestId));
        } catch (DataIntegrityViolationException duplicate) {
            throw duplicate;
        }
    }

    @Transactional(readOnly = true)
    public Booking byRequestId(String requestId) {
        return bookings.findByRequestId(requestId).orElseThrow(() -> new EntityNotFoundException("Booking not found"));
    }

    @Transactional
    public Booking confirm(String bookingId, Long roomId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (booking.getStatus() == BookingStatus.CANCELLED) throw new IllegalStateException("Cancelled booking cannot be confirmed");
        if (booking.getStatus() == BookingStatus.PENDING) {
            booking.setRoomId(roomId);
            booking.setStatus(BookingStatus.CONFIRMED);
        }
        return booking;
    }

    @Transactional
    public void assignPendingRoom(String bookingId, Long roomId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (booking.getStatus() == BookingStatus.PENDING) booking.setRoomId(roomId);
    }

    @Transactional
    public Booking cancel(String bookingId, String username, boolean administrator) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (!administrator && !booking.getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only manage your own bookings");
        }
        if (booking.getStatus() == BookingStatus.PENDING || booking.getStatus() == BookingStatus.CONFIRMED) {
            booking.setStatus(BookingStatus.CANCELLED);
        }
        if (booking.getRoomId() != null) booking.setReleasePending(true);
        return booking;
    }

    @Transactional
    public void markRoomReleased(String bookingId) {
        bookings.findById(bookingId).ifPresent(booking -> booking.setReleasePending(false));
    }

    @Transactional(readOnly = true)
    public java.util.List<Booking> pendingReleases() {
        return bookings.findTop100ByReleasePendingTrueOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public Booking getOwned(String id, String username, boolean administrator) {
        Booking booking = bookings.findById(id).orElseThrow(() -> new EntityNotFoundException("Booking not found"));
        if (!administrator && !booking.getUsername().equals(username)) {
            throw new org.springframework.security.access.AccessDeniedException("You can only view your own bookings");
        }
        return booking;
    }

    @Transactional(readOnly = true)
    public Page<Booking> history(String username, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bookings.findByUsernameOrderByCreatedAtDesc(username, pageable);
    }
}
