package dev.hotelbooking.bookings.api;

import dev.hotelbooking.bookings.model.Booking;
import dev.hotelbooking.bookings.service.BookingOrchestrator;
import dev.hotelbooking.bookings.service.BookingPersistence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Validated
public class BookingController {
    private final BookingOrchestrator orchestrator;
    private final BookingPersistence persistence;

    public BookingController(BookingOrchestrator orchestrator, BookingPersistence persistence) {
        this.orchestrator = orchestrator;
        this.persistence = persistence;
    }

    @PostMapping("/api/booking")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Booking create(@Valid @RequestBody BookingRequest request,
                          @RequestHeader(value = "Idempotency-Key", required = false) @Size(max = 100) String key,
                          @AuthenticationPrincipal Jwt jwt) {
        String requestId = StringUtils.hasText(key) ? key.trim() : UUID.randomUUID().toString();
        return orchestrator.create(jwt.getSubject(), request, requestId);
    }

    @GetMapping("/api/bookings")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<Booking> history(@RequestParam(defaultValue = "0") @Min(0) int page,
                                 @RequestParam(defaultValue = "20") @Min(1) int size,
                                 @AuthenticationPrincipal Jwt jwt) {
        return persistence.history(jwt.getSubject(), page, size);
    }

    @GetMapping("/api/booking/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Booking get(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        boolean admin = jwt.getClaimAsStringList("roles").contains("ADMIN");
        return persistence.getOwned(id, jwt.getSubject(), admin);
    }

    @DeleteMapping("/api/booking/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Booking cancel(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        boolean admin = jwt.getClaimAsStringList("roles").contains("ADMIN");
        return orchestrator.cancel(id, jwt.getSubject(), admin);
    }
}
