package dev.hotelbooking.bookings.config;

import dev.hotelbooking.bookings.model.User;
import dev.hotelbooking.bookings.repository.UserRepository;
import dev.hotelbooking.security.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DemoData {
    @Bean
    CommandLineRunner seedUsers(UserRepository users, PasswordEncoder encoder,
                               @Value("${app.seed.enabled:true}") boolean enabled,
                               @Value("${app.seed.admin-username:admin}") String adminName,
                               @Value("${app.seed.admin-password:Admin123!}") String adminPassword,
                               @Value("${app.seed.user-username:guest}") String userName,
                               @Value("${app.seed.user-password:Guest123!}") String userPassword) {
        return args -> {
            if (!enabled) return;
            if (!users.existsByUsername(adminName)) users.save(new User(adminName, encoder.encode(adminPassword), Role.ADMIN));
            if (!users.existsByUsername(userName)) users.save(new User(userName, encoder.encode(userPassword), Role.USER));
        };
    }
}
