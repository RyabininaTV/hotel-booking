package dev.hotelbooking.bookings.api;

import dev.hotelbooking.bookings.service.BookingOrchestrator.NoAvailableRoomException;
import dev.hotelbooking.bookings.service.HotelClient.HotelServiceUnavailableException;
import dev.hotelbooking.bookings.service.UserService.InvalidCredentialsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<ApiError> notFound(EntityNotFoundException e, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(NoAvailableRoomException.class)
    ResponseEntity<ApiError> noRoom(NoAvailableRoomException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ApiError> unauthorized(InvalidCredentialsException e, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid username or password", request, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(AccessDeniedException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(HotelServiceUnavailableException.class)
    ResponseEntity<ApiError> unavailable(HotelServiceUnavailableException e, HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException e, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> conflict(IllegalStateException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, e.getMessage(), request, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> duplicate(DataIntegrityViolationException e, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "A record with these identifiers already exists", request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e, HttpServletRequest request) {
        var errors = e.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(
                        field -> field.getField(), field -> field.getDefaultMessage() == null ? "invalid" : field.getDefaultMessage(),
                        (first, ignored) -> first));
        var globalErrors = e.getBindingResult().getGlobalErrors();
        if (!globalErrors.isEmpty()) errors.put("request", globalErrors.get(0).getDefaultMessage());
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", request, errors);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                message, request.getRequestURI(), fields));
    }
}
