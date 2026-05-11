package com.example.smart_cinema_booking_system.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookingRequest {
    private Integer showtimeId;
    private List<Integer> seatIds;
    private String paymentMethod;
}
