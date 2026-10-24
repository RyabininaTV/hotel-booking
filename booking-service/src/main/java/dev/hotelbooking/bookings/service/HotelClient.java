package dev.hotelbooking.bookings.service;

import dev.hotelbooking.security.JwtTokenService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

@Component
public class HotelClient {
    private static final Logger log = LoggerFactory.getLogger(HotelClient.class);
    private static final int MAX_ATTEMPTS = 3;
    private final RestClient restClient;
    private final JwtTokenService tokens;
    private final CircuitBreaker circuitBreaker = CircuitBreaker.of("hotel-service",
            CircuitBreakerConfig.custom().slidingWindowSize(10).minimumNumberOfCalls(3)
                    .failureRateThreshold(50).waitDurationInOpenState(Duration.ofSeconds(15))
                    .ignoreExceptions(RoomUnavailableException.class, HttpClientErrorException.class).build());

    public HotelClient(RestClient.Builder builder, JwtTokenService tokens) {
        this.restClient = builder.baseUrl("http://hotel-service").build();
        this.tokens = tokens;
    }

    public List<RoomCandidate> recommend(LocalDate checkIn, LocalDate checkOut) {
        return withResilience("recommend rooms", () -> restClient.get()
                .uri(uri -> uri.path("/api/rooms/recommend")
                        .queryParam("checkIn", checkIn).queryParam("checkOut", checkOut).build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issueInternalToken())
                .retrieve().body(new ParameterizedTypeReference<>() {}));
    }

    public RoomCandidate reserve(Long roomId, String bookingId, String requestId, LocalDate checkIn, LocalDate checkOut) {
        return withResilience("reserve room " + roomId, () -> restClient.post()
                .uri("/api/rooms/{id}/confirm-availability", roomId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issueInternalToken())
                .body(new ReserveRequest(bookingId, requestId, checkIn, checkOut))
                .retrieve()
                .onStatus(status -> status.value() == 409, (request, response) -> {
                    throw new RoomUnavailableException("Room is no longer available");
                })
                .body(RoomCandidate.class));
    }

    public void release(Long roomId, String bookingId, String requestId) {
        withResilience("release room " + roomId, () -> {
            restClient.post()
                    .uri("/api/rooms/{id}/release", roomId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.issueInternalToken())
                    .body(new ReleaseRequest(bookingId, requestId))
                    .retrieve()
                    .toBodilessEntity();
            return Boolean.TRUE;
        });
    }

    private <T> T withResilience(String operation, Supplier<T> action) {
        try {
            return circuitBreaker.executeSupplier(() -> retry(operation, action));
        } catch (CallNotPermittedException e) {
            throw new HotelServiceUnavailableException("Hotel service circuit is open", e);
        }
    }

    private <T> T retry(String operation, Supplier<T> action) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return action.get();
            } catch (RoomUnavailableException | HttpClientErrorException e) {
                throw e;
            } catch (RestClientException e) {
                last = e;
                log.warn("hotel_call_failed operation={} attempt={} maxAttempts={} reason={}",
                        operation, attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        Thread.sleep(150L * (1L << (attempt - 1)));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new HotelServiceUnavailableException("Interrupted while retrying Hotel Service", interrupted);
                    }
                }
            }
        }
        throw new HotelServiceUnavailableException("Hotel Service did not respond after " + MAX_ATTEMPTS + " attempts", last);
    }

    public record RoomCandidate(Long id, Long hotelId, String hotelName, String number, boolean available, long timesBooked) {}
    public record ReserveRequest(String bookingId, String requestId, LocalDate checkIn, LocalDate checkOut) {}
    public record ReleaseRequest(String bookingId, String requestId) {}

    public static class RoomUnavailableException extends RuntimeException {
        public RoomUnavailableException(String message) { super(message); }
    }

    public static class HotelServiceUnavailableException extends RuntimeException {
        public HotelServiceUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
