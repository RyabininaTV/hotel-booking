package dev.hotelbooking.hotels.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "room_stays",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_stay_request", columnNames = "request_id"),
        indexes = @Index(name = "idx_stay_room_dates", columnList = "room_id,start_date,end_date"))
public class RoomStay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "room_id", nullable = false)
    private Long roomId;
    @Column(name = "booking_id", nullable = false, unique = true, length = 36)
    private String bookingId;
    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    protected RoomStay() {}

    public RoomStay(Long roomId, String bookingId, String requestId, LocalDate startDate, LocalDate endDate) {
        this.roomId = roomId;
        this.bookingId = bookingId;
        this.requestId = requestId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() { return id; }
    public Long getRoomId() { return roomId; }
    public String getBookingId() { return bookingId; }
    public String getRequestId() { return requestId; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
}
