package dev.hotelbooking.bookings.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_request_id", columnNames = "request_id"),
        indexes = {
                @Index(name = "idx_booking_owner_created", columnList = "username,created_at"),
                @Index(name = "idx_booking_room_dates", columnList = "room_id,start_date,end_date")
        })
public class Booking {
    @Id
    @Column(length = 36)
    private String id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false, length = 80)
    private String username;
    @Column(name = "room_id")
    private Long roomId;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private BookingStatus status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;
    @Column(name = "release_pending", nullable = false)
    private boolean releasePending;

    protected Booking() {}

    public Booking(Long userId, String username, Long roomId, LocalDate startDate, LocalDate endDate, String requestId) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.username = username;
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.requestId = requestId;
        this.status = BookingStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public String getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public Long getRoomId() { return roomId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public BookingStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getRequestId() { return requestId; }
    public boolean isReleasePending() { return releasePending; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setReleasePending(boolean releasePending) { this.releasePending = releasePending; }
}
