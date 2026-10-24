package dev.hotelbooking.hotels.service;

import dev.hotelbooking.hotels.model.Hotel;
import dev.hotelbooking.hotels.model.Room;
import dev.hotelbooking.hotels.repository.HotelRepository;
import dev.hotelbooking.hotels.repository.RoomRepository;
import dev.hotelbooking.hotels.repository.RoomStayRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(HotelCatalogService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcurrentReservationTest {
    @Autowired HotelCatalogService service;
    @Autowired HotelRepository hotels;
    @Autowired RoomRepository rooms;
    @Autowired RoomStayRepository stays;

    @Test
    void concurrentOverlappingReservationsAllowOnlyOneWinner() throws Exception {
        Hotel hotel = hotels.saveAndFlush(new Hotel("Concurrency Test Hotel", "Test address"));
        Room room = rooms.saveAndFlush(new Room(hotel, "C-1", true));
        LocalDate checkIn = LocalDate.now().plusDays(20);
        LocalDate checkOut = checkIn.plusDays(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var first = executor.submit(() -> reserveAfterBarrier(ready, start, room.getId(), "parallel-booking-1",
                    "parallel-request-1", checkIn, checkOut));
            var second = executor.submit(() -> reserveAfterBarrier(ready, start, room.getId(), "parallel-booking-2",
                    "parallel-request-2", checkIn, checkOut));
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();

            int wins = (first.get(10, TimeUnit.SECONDS) ? 1 : 0) + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);

            assertEquals(1, wins);
            assertEquals(1, stays.countByRoomId(room.getId()));
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean reserveAfterBarrier(CountDownLatch ready, CountDownLatch start, Long roomId,
                                        String bookingId, String requestId, LocalDate checkIn, LocalDate checkOut) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Test start barrier timed out");
        try {
            service.reserve(roomId, bookingId, requestId, checkIn, checkOut);
            return true;
        } catch (HotelCatalogService.RoomUnavailableException expectedConflict) {
            return false;
        }
    }
}
