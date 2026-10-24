package dev.hotelbooking.hotels.config;

import dev.hotelbooking.hotels.model.Hotel;
import dev.hotelbooking.hotels.model.Room;
import dev.hotelbooking.hotels.repository.HotelRepository;
import dev.hotelbooking.hotels.repository.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoData {
    @Bean
    CommandLineRunner seedHotels(HotelRepository hotels, RoomRepository rooms) {
        return args -> {
            if (hotels.count() > 0) return;
            Hotel north = hotels.save(new Hotel("North Star Hotel", "12 Nevsky Avenue, Saint Petersburg"));
            Hotel river = hotels.save(new Hotel("Riverlight Residence", "8 Sofiyskaya Embankment, Kazan"));
            rooms.save(new Room(north, "101", true));
            rooms.save(new Room(north, "102", true));
            rooms.save(new Room(north, "201", true));
            rooms.save(new Room(river, "14", true));
        };
    }
}
