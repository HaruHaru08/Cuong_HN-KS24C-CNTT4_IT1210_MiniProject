package com.example.smart_cinema_booking_system.exception;

import java.util.List;

public class BookingConflictException extends RuntimeException {

    private final List<ConflictSeatInfo> conflictSeats;

    public BookingConflictException(String message, List<ConflictSeatInfo> conflictSeats) {
        super(message);
        this.conflictSeats = conflictSeats;
    }

    public List<ConflictSeatInfo> getConflictSeats() {
        return conflictSeats;
    }

    public static class ConflictSeatInfo {
        private final Integer seatId;
        private final String seatNumber;
        private final String message;

        public ConflictSeatInfo(Integer seatId, String seatNumber, String message) {
            this.seatId = seatId;
            this.seatNumber = seatNumber;
            this.message = message;
        }

        public Integer getSeatId() { return seatId; }
        public String getSeatNumber() { return seatNumber; }
        public String getMessage() { return message; }
    }
}