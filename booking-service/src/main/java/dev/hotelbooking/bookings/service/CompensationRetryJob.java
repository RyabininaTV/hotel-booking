package dev.hotelbooking.bookings.service;

import dev.hotelbooking.bookings.model.Booking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CompensationRetryJob {
    private static final Logger log = LoggerFactory.getLogger(CompensationRetryJob.class);
    private final BookingPersistence persistence;
    private final HotelClient hotelClient;

    public CompensationRetryJob(BookingPersistence persistence, HotelClient hotelClient) {
        this.persistence = persistence;
        this.hotelClient = hotelClient;
    }

    @Scheduled(fixedDelayString = "${app.saga.compensation-retry-ms:10000}")
    public void retryPendingReleases() {
        for (Booking booking : persistence.pendingReleases()) {
            try {
                hotelClient.release(booking.getRoomId(), booking.getId(), booking.getRequestId());
                persistence.markRoomReleased(booking.getId());
                log.info("compensation_retry_succeeded bookingId={} roomId={}", booking.getId(), booking.getRoomId());
            } catch (RuntimeException error) {
                log.warn("compensation_retry_failed bookingId={} reason={}", booking.getId(), error.getMessage());
            }
        }
    }
}
