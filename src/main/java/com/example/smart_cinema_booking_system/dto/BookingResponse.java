package com.example.smart_cinema_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {
    private boolean success;
    private String message;
    private Integer bookingId;
    private Double totalAmount;
    private List<Map<String, Object>> conflictSeats;
}
