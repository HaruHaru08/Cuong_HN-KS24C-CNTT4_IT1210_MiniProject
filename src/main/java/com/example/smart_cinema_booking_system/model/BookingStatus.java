package com.example.smart_cinema_booking_system.model;

public enum BookingStatus {
    PENDING,   // Chờ thanh toán
    PAID,      // Đã thanh toán thành công
    CANCELLED  // Đã hủy (do khách chủ động hoặc quá thời gian)
}
