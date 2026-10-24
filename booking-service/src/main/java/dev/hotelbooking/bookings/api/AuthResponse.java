package dev.hotelbooking.bookings.api;

import dev.hotelbooking.security.Role;

public record AuthResponse(String token, String tokenType, long expiresInSeconds, UserView user) {
    public record UserView(Long id, String username, Role role) {}
}
