package com.example.smart_cinema_booking_system.repository;

import com.example.smart_cinema_booking_system.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    
    /**
     * Tìm tất cả booking của một user
     */
    List<Booking> findByUserId(Integer userId);
    
    /**
     * Tìm booking gần đây nhất của user
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.bookingDate DESC LIMIT 1")
    Booking findLatestByUserId(@Param("userId") Integer userId);
    
    /**
     * Tìm booking theo user và ngày
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND DATE(b.bookingDate) = DATE(:date)")
    List<Booking> findByUserIdAndDate(@Param("userId") Integer userId, @Param("date") LocalDateTime date);
    
    /**
     * Lấy lịch sử đặt vé của user với JOIN 5 bảng:
     * Booking → Ticket → Showtime → Movie, Room, Seat
     * Sắp xếp theo ngày đặt mới nhất
     */
    @Query(value = """
        SELECT
            b.id AS bookingId,
            m.title AS movieTitle,
            m.poster_url AS moviePosterUrl,
            s.start_time AS showStartTime,
            r.name AS roomName,
            GROUP_CONCAT(se.seat_number ORDER BY se.seat_number SEPARATOR ', ') AS listSeats,
            b.total_amount AS totalPrice,
            b.booking_date AS bookingDate,
            b.status AS status
        FROM bookings b
        JOIN tickets t ON b.id = t.booking_id
        JOIN showtimes s ON t.showtime_id = s.id
        JOIN movies m ON s.movie_id = m.id
        JOIN rooms r ON s.room_id = r.id
        JOIN seats se ON t.seat_id = se.id
        WHERE b.user_id = :userId
        GROUP BY b.id, m.title, m.poster_url, s.start_time, r.name, b.total_amount, b.booking_date, b.status
        ORDER BY b.booking_date DESC
    """, nativeQuery = true)
    List<Object[]> findBookingHistoryByUserId(@Param("userId") Integer userId);
}
