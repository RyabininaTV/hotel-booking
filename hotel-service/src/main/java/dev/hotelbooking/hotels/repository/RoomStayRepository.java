package dev.hotelbooking.hotels.repository;

import dev.hotelbooking.hotels.model.RoomStay;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomStayRepository extends JpaRepository<RoomStay, Long> {
    Optional<RoomStay> findByRequestId(String requestId);
    Optional<RoomStay> findByBookingId(String bookingId);
    List<RoomStay> findByRoomIdAndStartDateLessThanAndEndDateGreaterThan(
            Long roomId, LocalDate requestedEnd, LocalDate requestedStart);
    long countByRoomId(Long roomId);
}
