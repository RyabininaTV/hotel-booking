package dev.hotelbooking.hotels.service;

import dev.hotelbooking.hotels.api.HotelRequest;
import dev.hotelbooking.hotels.api.RoomRequest;
import dev.hotelbooking.hotels.api.RoomResponse;
import dev.hotelbooking.hotels.model.Hotel;
import dev.hotelbooking.hotels.model.Room;
import dev.hotelbooking.hotels.repository.HotelRepository;
import dev.hotelbooking.hotels.repository.RoomRepository;
import dev.hotelbooking.hotels.repository.RoomStayRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class HotelCatalogService {
    private static final Logger log = LoggerFactory.getLogger(HotelCatalogService.class);
    private final HotelRepository hotels;
    private final RoomRepository rooms;
    private final RoomStayRepository stays;

    public HotelCatalogService(HotelRepository hotels, RoomRepository rooms, RoomStayRepository stays) {
        this.hotels = hotels;
        this.rooms = rooms;
        this.stays = stays;
    }

    public List<Hotel> hotels() {
        return hotels.findAll();
    }

    public Hotel hotel(Long id) {
        return hotels.findById(id).orElseThrow(() -> new EntityNotFoundException("Hotel not found"));
    }

    @Transactional
    public Hotel createHotel(HotelRequest request) {
        return hotels.save(new Hotel(request.name().trim(), request.address().trim()));
    }

    @Transactional
    public Hotel updateHotel(Long id, HotelRequest request) {
        Hotel hotel = hotel(id);
        hotel.setName(request.name().trim());
        hotel.setAddress(request.address().trim());
        return hotel;
    }

    @Transactional
    public void deleteHotel(Long id) {
        Hotel hotel = hotel(id);
        if (rooms.countByHotelId(id) > 0) {
            throw new IllegalStateException("Delete the hotel's rooms before deleting the hotel");
        }
        hotels.delete(hotel);
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        Hotel hotel = hotel(request.hotelId());
        Room room = rooms.save(new Room(hotel, request.number().trim(), request.available() == null || request.available()));
        return response(room);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = room(id);
        room.setHotel(hotel(request.hotelId()));
        room.setNumber(request.number().trim());
        if (request.available() != null) room.setAvailable(request.available());
        return response(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = room(id);
        if (stays.countByRoomId(id) > 0) {
            throw new IllegalStateException("A room with existing stays cannot be deleted");
        }
        rooms.delete(room);
    }

    public RoomResponse roomResponse(Long id) {
        return response(room(id));
    }

    public List<RoomResponse> availableRooms(Long hotelId, LocalDate checkIn, LocalDate checkOut, boolean recommended) {
        validateDates(checkIn, checkOut);
        List<Room> candidates = hotelId == null ? rooms.findByAvailableTrue() : rooms.findByAvailableTrueAndHotelId(hotelId);
        return candidates.stream()
                .filter(room -> checkIn == null || stays.findByRoomIdAndStartDateLessThanAndEndDateGreaterThan(
                        room.getId(), checkOut, checkIn).isEmpty())
                .sorted(recommended
                        ? Comparator.comparingLong(Room::getTimesBooked).thenComparing(Room::getId)
                        : Comparator.comparing(Room::getId))
                .map(this::response)
                .toList();
    }

    @Transactional
    public RoomResponse reserve(Long roomId, String bookingId, String requestId, LocalDate checkIn, LocalDate checkOut) {
        validateDates(checkIn, checkOut);
        var priorRequest = stays.findByRequestId(requestId);
        if (priorRequest.isPresent()) {
            var existing = priorRequest.get();
            if (!existing.getBookingId().equals(bookingId) || !existing.getRoomId().equals(roomId)
                    || !existing.getStartDate().equals(checkIn) || !existing.getEndDate().equals(checkOut)) {
                throw new IllegalStateException("requestId was already used for a different reservation");
            }
            log.info("hotel_reservation_idempotent_replay bookingId={} roomId={} requestId={}",
                    bookingId, roomId, requestId);
            return response(room(roomId));
        }

        Room room = rooms.findLockedById(roomId).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        if (!room.isAvailable()) throw new RoomUnavailableException("Room is out of service");
        if (stays.findByBookingId(bookingId).isPresent()) {
            throw new IllegalStateException("bookingId is already associated with a different room stay");
        }
        if (!stays.findByRoomIdAndStartDateLessThanAndEndDateGreaterThan(roomId, checkOut, checkIn).isEmpty()) {
            throw new RoomUnavailableException("Room is already reserved for part of the requested period");
        }
        stays.saveAndFlush(new dev.hotelbooking.hotels.model.RoomStay(roomId, bookingId, requestId, checkIn, checkOut));
        room.setTimesBooked(room.getTimesBooked() + 1);
        log.info("hotel_reservation_created bookingId={} roomId={} requestId={}", bookingId, roomId, requestId);
        return response(room);
    }

    @Transactional
    public void release(Long roomId, String bookingId, String requestId) {
        var stay = stays.findByRequestId(requestId).or(() -> stays.findByBookingId(bookingId));
        if (stay.isEmpty()) return;
        var reservation = stay.get();
        if (!reservation.getRoomId().equals(roomId) || !reservation.getBookingId().equals(bookingId)) {
            throw new IllegalStateException("Reservation identifiers do not match");
        }
        Room room = rooms.findLockedById(roomId).orElseThrow(() -> new EntityNotFoundException("Room not found"));
        stays.delete(reservation);
        room.setTimesBooked(Math.max(0, room.getTimesBooked() - 1));
        log.info("hotel_reservation_released bookingId={} roomId={} requestId={}", bookingId, roomId, requestId);
    }

    public List<Occupancy> occupancy() {
        return hotels.findAll().stream().map(hotel -> {
            var hotelRooms = rooms.findByAvailableTrueAndHotelId(hotel.getId());
            long reservations = hotelRooms.stream().mapToLong(room -> stays.countByRoomId(room.getId())).sum();
            return new Occupancy(hotel.getId(), hotel.getName(), hotelRooms.size(), reservations,
                    hotelRooms.isEmpty() ? 0 : Math.round((double) reservations / hotelRooms.size() * 100));
        }).toList();
    }

    private Room room(Long id) {
        return rooms.findById(id).orElseThrow(() -> new EntityNotFoundException("Room not found"));
    }

    private RoomResponse response(Room room) {
        return new RoomResponse(room.getId(), room.getHotel().getId(), room.getHotel().getName(),
                room.getNumber(), room.isAvailable(), room.getTimesBooked());
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if ((checkIn == null) != (checkOut == null)) throw new IllegalArgumentException("checkIn and checkOut must be supplied together");
        if (checkIn != null && !checkOut.isAfter(checkIn)) throw new IllegalArgumentException("checkOut must be after checkIn");
    }

    public record Occupancy(Long hotelId, String hotelName, long availableRooms, long activeReservations, long occupancyPercent) {}

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String message) { super(message); }
    }
}
