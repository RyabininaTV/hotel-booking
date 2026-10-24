package dev.hotelbooking.hotels.api;

import dev.hotelbooking.hotels.service.HotelCatalogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {
    private final HotelCatalogService service;

    public RoomController(HotelCatalogService service) { this.service = service; }

    @GetMapping
    public List<RoomResponse> available(@RequestParam(required = false) Long hotelId,
                                        @RequestParam(required = false) LocalDate checkIn,
                                        @RequestParam(required = false) LocalDate checkOut) {
        return service.availableRooms(hotelId, checkIn, checkOut, false);
    }

    @GetMapping("/recommend")
    public List<RoomResponse> recommend(@RequestParam LocalDate checkIn,
                                        @RequestParam LocalDate checkOut,
                                        @RequestParam(required = false) Long hotelId) {
        return service.availableRooms(hotelId, checkIn, checkOut, true);
    }

    @GetMapping("/{id}")
    public RoomResponse room(@PathVariable Long id) { return service.roomResponse(id); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse create(@Valid @RequestBody RoomRequest request) { return service.createRoom(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomResponse update(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return service.updateRoom(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) { service.deleteRoom(id); }

    @PostMapping("/{id}/confirm-availability")
    @PreAuthorize("hasRole('INTERNAL')")
    public RoomResponse reserve(@PathVariable Long id, @Valid @RequestBody AvailabilityRequest request) {
        return service.reserve(id, request.bookingId(), request.requestId(), request.checkIn(), request.checkOut());
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasRole('INTERNAL')")
    public void release(@PathVariable Long id, @Valid @RequestBody ReleaseRequest request) {
        service.release(id, request.bookingId(), request.requestId());
    }

    public record ReleaseRequest(String bookingId, String requestId) {}
}
