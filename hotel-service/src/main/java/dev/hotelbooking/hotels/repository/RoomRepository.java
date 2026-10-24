package dev.hotelbooking.hotels.repository;

import dev.hotelbooking.hotels.model.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findLockedById(@Param("id") Long id);

    List<Room> findByAvailableTrue();
    List<Room> findByAvailableTrueAndHotelId(Long hotelId);
    long countByHotelId(Long hotelId);
}
