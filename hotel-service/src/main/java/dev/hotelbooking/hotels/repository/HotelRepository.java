package dev.hotelbooking.hotels.repository;

import dev.hotelbooking.hotels.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {}
