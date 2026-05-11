package com.example.smart_cinema_booking_system.repository;

import com.example.smart_cinema_booking_system.model.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Integer> {
    
    /**
     * Tìm tất cả các suất chiếu của một phòng
     */
    List<Showtime> findByRoomId(Integer roomId);
    
    /**
     * Tìm tất cả các suất chiếu của một phim
     */
    List<Showtime> findByMovieId(Integer movieId);
    
    /**
     * Kiểm tra xung đột suất chiếu trong phòng
     * Sử dụng logic: (newStart < existingEnd) AND (newEnd > existingStart)
     * 
     * @param roomId ID phòng chiếu
     * @param startTime Thời gian bắt đầu suất chiếu mới
     * @param endTime Thời gian kết thúc suất chiếu mới
     * @return Danh sách các suất chiếu bị xung đột
     */
    @Query("SELECT s FROM Showtime s WHERE s.room.id = :roomId " +
           "AND s.startTime < :endTime AND s.endTime > :startTime " +
           "AND s.status = true")
    List<Showtime> findConflictingShowtimes(
            @Param("roomId") Integer roomId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Tìm các suất chiếu của phòng trong khoảng thời gian nhất định
     */
    @Query("SELECT s FROM Showtime s WHERE s.room.id = :roomId " +
           "AND s.startTime >= :startDate AND s.startTime < :endDate " +
           "AND s.status = true ORDER BY s.startTime ASC")
    List<Showtime> findShowtimesByRoomAndDateRange(
            @Param("roomId") Integer roomId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Tìm tất cả các suất chiếu trong khoảng thời gian nhất định
     */
    @Query("SELECT s FROM Showtime s WHERE s.startTime >= :startDate " +
           "AND s.startTime < :endDate AND s.status = true ORDER BY s.room.id ASC, s.startTime ASC")
    List<Showtime> findShowtimesByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
