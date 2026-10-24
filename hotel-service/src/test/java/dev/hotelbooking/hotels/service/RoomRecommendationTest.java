package dev.hotelbooking.hotels.service;

import dev.hotelbooking.hotels.model.Hotel;
import dev.hotelbooking.hotels.model.Room;
import dev.hotelbooking.hotels.repository.HotelRepository;
import dev.hotelbooking.hotels.repository.RoomRepository;
import dev.hotelbooking.hotels.repository.RoomStayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomRecommendationTest {
    @Mock HotelRepository hotels;
    @Mock RoomRepository rooms;
    @Mock RoomStayRepository stays;

    @Test
    void recommendationsSortByBookingCountThenRoomId() {
        Hotel hotel = mock(Hotel.class);
        when(hotel.getId()).thenReturn(1L);
        when(hotel.getName()).thenReturn("Test hotel");
        Room roomA = room(9L, "9", 3, hotel);
        Room roomB = room(4L, "4", 1, hotel);
        Room roomC = room(2L, "2", 1, hotel);
        when(rooms.findByAvailableTrue()).thenReturn(List.of(roomA, roomB, roomC));
        when(stays.findByRoomIdAndStartDateLessThanAndEndDateGreaterThan(anyLong(), any(), any())).thenReturn(List.of());
        HotelCatalogService service = new HotelCatalogService(hotels, rooms, stays);

        var result = service.availableRooms(null, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), true);

        assertEquals(List.of(2L, 4L, 9L), result.stream().map(RoomResponse -> RoomResponse.id()).toList());
    }

    private Room room(long id, String number, long timesBooked, Hotel hotel) {
        Room room = mock(Room.class);
        when(room.getId()).thenReturn(id);
        when(room.getNumber()).thenReturn(number);
        when(room.getTimesBooked()).thenReturn(timesBooked);
        when(room.getHotel()).thenReturn(hotel);
        when(room.isAvailable()).thenReturn(true);
        return room;
    }
}
