package dev.hotelbooking.bookings.api;

import dev.hotelbooking.bookings.service.UserService;
import dev.hotelbooking.bookings.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.jwt.secret=01234567890123456789012345678901")
class BookingApiSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean UserService users;

    @Test
    void adminUserApiRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/user")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationRejectsInvalidCredentialsBeforeCallingService() throws Exception {
        mvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(users);
    }
}
