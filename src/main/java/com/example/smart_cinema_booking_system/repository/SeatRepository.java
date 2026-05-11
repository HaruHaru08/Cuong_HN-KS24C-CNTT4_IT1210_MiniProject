package com.example.smart_cinema_booking_system.repository;

import com.example.smart_cinema_booking_system.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {
    
    /**
     * Tìm tất cả ghế trong một phòng
     */
    List<Seat> findByRoomId(Integer roomId);
    
    /**
     * Tìm ghế theo phòng và số hiệu ghế
     */
    Optional<Seat> findByRoomIdAndSeatNumber(Integer roomId, String seatNumber);
    
    /**
     * Lấy danh sách ghế đã được đặt cho một suất chiếu
     */
    @Query("SELECT t.seat FROM Ticket t WHERE t.showtime.id = :showtimeId")
    List<Seat> findBookedSeatsByShowtime(@Param("showtimeId") Integer showtimeId);
    
    /**
     * Kiểm tra xem ghế có bị đặt không
     */
    @Query("SELECT COUNT(t) > 0 FROM Ticket t WHERE t.seat.id = :seatId AND t.showtime.id = :showtimeId")
    boolean isSeatBookedForShowtime(@Param("seatId") Integer seatId, @Param("showtimeId") Integer showtimeId);
    
    /**
     * Xóa tất cả ghế của một phòng
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Seat s WHERE s.room.id = :roomId")
    void deleteByRoomId(@Param("roomId") Integer roomId);

    /**
     * Đếm số ghế của một phòng
     */
    long countByRoomId(Integer roomId);

    /**
     * Lấy ghế với Pessimistic Lock (dùng khi booking để chặn race condition)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :seatId")
    Optional<Seat> findByIdWithLock(@Param("seatId") Integer seatId);
    
    /**
     * Lấy danh sách ghế với lock
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")
    List<Seat> findAllByIdWithLock(@Param("seatIds") List<Integer> seatIds);
}
