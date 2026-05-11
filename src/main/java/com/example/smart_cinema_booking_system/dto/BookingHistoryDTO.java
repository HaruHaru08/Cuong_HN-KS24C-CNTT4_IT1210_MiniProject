package com.example.smart_cinema_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingHistoryDTO {
    private Integer bookingId;
    private String movieTitle;
    private String moviePosterUrl;
    private LocalDateTime showStartTime;
    private String roomName;
    private String listSeats;
    private Double totalPrice;
    private LocalDateTime bookingDate;
    private String status;
}
