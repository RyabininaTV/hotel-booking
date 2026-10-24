package dev.hotelbooking.hotels.api;

import dev.hotelbooking.hotels.model.Hotel;
import dev.hotelbooking.hotels.service.HotelCatalogService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {
    private final HotelCatalogService service;

    public HotelController(HotelCatalogService service) { this.service = service; }

    @GetMapping
    public List<Hotel> hotels() { return service.hotels(); }

    @GetMapping("/{id}")
    public Hotel hotel(@PathVariable Long id) { return service.hotel(id); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Hotel create(@Valid @RequestBody HotelRequest request) { return service.createHotel(request); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Hotel update(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
        return service.updateHotel(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) { service.deleteHotel(id); }

    @GetMapping("/occupancy")
    @PreAuthorize("hasRole('ADMIN')")
    public List<HotelCatalogService.Occupancy> occupancy() { return service.occupancy(); }
}
