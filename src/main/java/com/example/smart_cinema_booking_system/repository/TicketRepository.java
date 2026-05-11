package com.example.smart_cinema_booking_system.repository;

import com.example.smart_cinema_booking_system.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    
    /**
     * Tìm tất cả vé của một booking
     */
    List<Ticket> findByBookingId(Integer bookingId);
    
    /**
     * Tìm tất cả vé của một suất chiếu
     */
    List<Ticket> findByShowtimeId(Integer showtimeId);
    
    /**
     * Kiểm tra vé có tồn tại không
     */
    @Query("SELECT COUNT(t) > 0 FROM Ticket t WHERE t.showtime.id = :showtimeId AND t.seat.id = :seatId")
    boolean exists(@Param("showtimeId") Integer showtimeId, @Param("seatId") Integer seatId);

    /**
     * Đếm số lượng vé đã đặt của một suất chiếu
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.showtime.id = :showtimeId")
    int countByShowtimeId(@Param("showtimeId") Integer showtimeId);

    /**
     * Kiểm tra suất chiếu có vé nào đã thanh toán không
     */
    @Query("SELECT COUNT(t) > 0 FROM Ticket t JOIN t.booking b WHERE t.showtime.id = :showtimeId AND b.status = 'PAID'")
    boolean hasPaidBookings(@Param("showtimeId") Integer showtimeId);

    /**
     * Tìm tất cả vé của một suất chiếu, chỉ lấy từ booking không phải CANCELLED
     * Dùng cho việc hiển thị sơ đồ ghế và kiểm tra xung đột
     */
    @Query("SELECT t FROM Ticket t JOIN t.booking b WHERE t.showtime.id = :showtimeId AND b.status <> 'CANCELLED'")
    List<Ticket> findActiveByShowtimeId(@Param("showtimeId") Integer showtimeId);

    /**
     * Đếm số vé đã bán (không tính CANCELLED) của một suất chiếu
     * Dùng cho logic Sold Out
     */
    @Query("SELECT COUNT(t) FROM Ticket t JOIN t.booking b WHERE t.showtime.id = :showtimeId AND b.status = 'PAID'")
    int countPaidByShowtimeId(@Param("showtimeId") Integer showtimeId);

}
