package dev.hotelbooking.bookings.service;

import dev.hotelbooking.bookings.api.AdminUserRequest;
import dev.hotelbooking.bookings.api.AuthRequest;
import dev.hotelbooking.bookings.api.AuthResponse;
import dev.hotelbooking.bookings.api.UserPatchRequest;
import dev.hotelbooking.bookings.model.User;
import dev.hotelbooking.bookings.repository.UserRepository;
import dev.hotelbooking.security.JwtTokenService;
import dev.hotelbooking.security.Role;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, JwtTokenService tokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String username = normalize(request.username());
        if (users.existsByUsername(username)) throw new IllegalStateException("Username is already in use");
        User user = users.save(new User(username, passwordEncoder.encode(request.password()), Role.USER));
        return response(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse authenticate(AuthRequest request) {
        User user = users.findByUsername(normalize(request.username()))
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new InvalidCredentialsException());
        return response(user);
    }

    @Transactional(readOnly = true)
    public List<AuthResponse.UserView> all() {
        return users.findAll().stream().map(this::view).toList();
    }

    @Transactional
    public AuthResponse.UserView create(AdminUserRequest request) {
        String username = normalize(request.username());
        if (users.existsByUsername(username)) throw new IllegalStateException("Username is already in use");
        Role role = request.role() == null ? Role.USER : request.role();
        if (role == Role.INTERNAL) throw new IllegalArgumentException("INTERNAL is reserved for service-to-service requests");
        return view(users.save(new User(username, passwordEncoder.encode(request.password()), role)));
    }

    @Transactional
    public AuthResponse.UserView update(Long id, UserPatchRequest request) {
        User user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (request.username() != null && !request.username().isBlank()) {
            String username = normalize(request.username());
            if (!username.equals(user.getUsername()) && users.existsByUsername(username)) {
                throw new IllegalStateException("Username is already in use");
            }
            user.setUsername(username);
        }
        if (request.password() != null && !request.password().isBlank()) user.setPassword(passwordEncoder.encode(request.password()));
        if (request.role() != null) {
            if (request.role() == Role.INTERNAL) throw new IllegalArgumentException("INTERNAL is reserved for service-to-service requests");
            user.setRole(request.role());
        }
        return view(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = users.findById(id).orElseThrow(() -> new EntityNotFoundException("User not found"));
        users.delete(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse.UserView get(Long id) {
        return users.findById(id).map(this::view).orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private AuthResponse response(User user) {
        return new AuthResponse(tokens.issue(user.getUsername(), user.getRole()), "Bearer", 3600, view(user));
    }

    private AuthResponse.UserView view(User user) {
        return new AuthResponse.UserView(user.getId(), user.getUsername(), user.getRole());
    }

    private String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public static class InvalidCredentialsException extends RuntimeException {}
}
