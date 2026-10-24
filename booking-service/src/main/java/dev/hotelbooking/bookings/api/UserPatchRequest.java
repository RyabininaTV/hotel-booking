package dev.hotelbooking.bookings.api;

import dev.hotelbooking.security.Role;
import jakarta.validation.constraints.Size;

public record UserPatchRequest(
        @Size(min = 3, max = 80) String username,
        @Size(min = 8, max = 100) String password,
        Role role) {}
