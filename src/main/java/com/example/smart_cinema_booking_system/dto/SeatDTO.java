package com.example.smart_cinema_booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatDTO {
    private Integer id;
    private String seatNumber;
    private String typeName;
    private double basePrice;
    private boolean isBooked;
    private String seatStatus; // "available", "booked", "selected"
}
