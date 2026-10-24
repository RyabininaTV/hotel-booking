package dev.hotelbooking.bookings.repository;

import dev.hotelbooking.bookings.model.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, String> {
    Optional<Booking> findByRequestId(String requestId);
    Page<Booking> findByUsernameOrderByCreatedAtDesc(String username, Pageable pageable);
    List<Booking> findTop100ByReleasePendingTrueOrderByCreatedAtAsc();
}
