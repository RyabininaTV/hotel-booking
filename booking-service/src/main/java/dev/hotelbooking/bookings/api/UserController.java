package dev.hotelbooking.bookings.api;

import dev.hotelbooking.bookings.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService users;

    public UserController(UserService users) { this.users = users; }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody AuthRequest request) { return users.register(request); }

    @PostMapping("/auth")
    public AuthResponse authenticate(@Valid @RequestBody AuthRequest request) { return users.authenticate(request); }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AuthResponse.UserView> list() { return users.all(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthResponse.UserView get(@PathVariable Long id) { return users.get(id); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AuthResponse.UserView create(@Valid @RequestBody AdminUserRequest request) { return users.create(request); }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthResponse.UserView update(@PathVariable Long id, @Valid @RequestBody UserPatchRequest request) {
        return users.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) { users.delete(id); }
}
