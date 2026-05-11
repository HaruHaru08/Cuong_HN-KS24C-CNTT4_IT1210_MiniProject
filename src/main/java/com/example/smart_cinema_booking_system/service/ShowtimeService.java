package com.example.smart_cinema_booking_system.service;

import com.example.smart_cinema_booking_system.dto.ShowtimeDTO;
import com.example.smart_cinema_booking_system.model.Movie;
import com.example.smart_cinema_booking_system.model.Room;
import com.example.smart_cinema_booking_system.model.Showtime;
import com.example.smart_cinema_booking_system.repository.MovieRepository;
import com.example.smart_cinema_booking_system.repository.RoomRepository;
import com.example.smart_cinema_booking_system.repository.ShowtimeRepository;
import com.example.smart_cinema_booking_system.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service để quản lý suất chiếu
 * 
 * Logic kiểm tra xung đột:
 * - Một phòng không được có 2 suất chiếu chồng chéo thời gian
 * - Thuật toán: (newStart < existingEnd) AND (newEnd > existingStart)
 * - Buffer time (dọn phòng): 15 phút sau mỗi suất chiếu
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeService {
    
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final TicketRepository ticketRepository;
    
    // Buffer time để dọn phòng sau suất chiếu (phút)
    private static final Integer BUFFER_TIME_MINUTES = 15;
    
    /**
     * Lấy tất cả các suất chiếu
     */
    public List<Showtime> getAllShowtimes() {
        return showtimeRepository.findAll();
    }
    
    /**
     * Lấy suất chiếu theo ID
     */
    public Showtime getShowtimeById(Integer id) {
        return showtimeRepository.findById(id).orElse(null);
    }
    
    /**
     * Lấy tất cả suất chiếu theo phòng
     */
    public List<Showtime> getShowtimesByRoom(Integer roomId) {
        return showtimeRepository.findByRoomId(roomId);
    }
    
    /**
     * Lấy tất cả suất chiếu theo phim
     */
    public List<Showtime> getShowtimesByMovie(Integer movieId) {
        return showtimeRepository.findByMovieId(movieId);
    }
    
    /**
     * Lấy các suất chiếu theo khoảng thời gian cho một phòng
     */
    public List<Showtime> getShowtimesByRoomAndDate(Integer roomId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return showtimeRepository.findShowtimesByRoomAndDateRange(roomId, startOfDay, endOfDay);
    }
    
    /**
     * Lấy tất cả các suất chiếu theo ngày
     */
    public List<Showtime> getShowtimesByDate(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
        return showtimeRepository.findShowtimesByDateRange(startOfDay, endOfDay);
    }
    
    /**
     * Lấy tất cả các suất chiếu theo tháng - sắp xếp theo phòng và giờ
     */
    public Map<Room, List<Showtime>> getShowtimesByMonth(YearMonth yearMonth) {
        LocalDateTime startOfMonth = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        List<Showtime> showtimes = showtimeRepository.findShowtimesByDateRange(startOfMonth, endOfMonth);
        
        return showtimes.stream().collect(Collectors.groupingBy(Showtime::getRoom));
    }
    
    /**
     * Kiểm tra xung đột suất chiếu
     * 
     * Logic: 
     * - Suất chiếu A và B xung đột nếu: A.startTime < B.endTime AND A.endTime > B.startTime
     * 
     * @param roomId ID phòng
     * @param startTime Thời gian bắt đầu suất chiếu mới
     * @param endTime Thời gian kết thúc suất chiếu mới
     * @param excludeShowtimeId ID suất chiếu để loại trừ (dùng khi update, null khi create)
     * @return true nếu có xung đột, false nếu không
     */
    public boolean hasConflict(Integer roomId, LocalDateTime startTime, LocalDateTime endTime, Integer excludeShowtimeId) {
        List<Showtime> conflicts = showtimeRepository.findConflictingShowtimes(roomId, startTime, endTime);
        
        // Loại trừ suất chiếu hiện tại nếu đang update
        if (excludeShowtimeId != null) {
            conflicts = conflicts.stream()
                    .filter(s -> !s.getId().equals(excludeShowtimeId))
                    .collect(Collectors.toList());
        }
        
        return !conflicts.isEmpty();
    }
    
    /**
     * Lấy danh sách xung đột cho hiển thị thông báo
     */
    public List<Showtime> getConflictingShowtimes(Integer roomId, LocalDateTime startTime, LocalDateTime endTime) {
        return showtimeRepository.findConflictingShowtimes(roomId, startTime, endTime);
    }
    
    /**
     * Tính toán endTime từ startTime và duration của phim
     * endTime = startTime + duration + buffer time
     * 
     * @param startTime Thời gian bắt đầu
     * @param movieDuration Thời lượng phim (phút)
     * @return Thời gian kết thúc
     */
    public LocalDateTime calculateEndTime(LocalDateTime startTime, Integer movieDuration) {
        if (movieDuration == null || movieDuration <= 0) {
            movieDuration = 120; // Default 2 giờ
        }
        return startTime.plusMinutes(movieDuration + BUFFER_TIME_MINUTES);
    }
    
    /**
     * Tạo mới suất chiếu
     */
    @Transactional
    public ShowtimeDTO createShowtime(ShowtimeDTO dto) {
        // Validate
        if (dto.getMovieId() == null || dto.getRoomId() == null || dto.getStartTime() == null) {
            dto.setErrorMessage("Thông tin suất chiếu không hợp lệ");
            dto.setHasConflict(false);
            return dto;
        }
        
        // Lấy thông tin phim
        Movie movie = movieRepository.findById(dto.getMovieId()).orElse(null);
        if (movie == null) {
            dto.setErrorMessage("Phim không tồn tại");
            dto.setHasConflict(false);
            return dto;
        }
        
        // Lấy thông tin phòng
        Room room = roomRepository.findById(dto.getRoomId()).orElse(null);
        if (room == null) {
            dto.setErrorMessage("Phòng chiếu không tồn tại");
            dto.setHasConflict(false);
            return dto;
        }
        
        // Tính toán endTime
        LocalDateTime endTime = calculateEndTime(dto.getStartTime(), movie.getDuration());
        dto.setEndTime(endTime);
        
        // Kiểm tra xung đột
        if (hasConflict(dto.getRoomId(), dto.getStartTime(), endTime, null)) {
            dto.setHasConflict(true);
            List<Showtime> conflicts = getConflictingShowtimes(dto.getRoomId(), dto.getStartTime(), endTime);
            StringBuilder conflictMsg = new StringBuilder("⚠️ Xung đột suất chiếu: Phòng " + room.getName() + " đã có suất chiếu vào:\n");
            for (Showtime conflict : conflicts) {
                conflictMsg.append("- ").append(conflict.getMovie().getTitle())
                        .append(" (").append(conflict.getStartTime()).append(")\n");
            }
            dto.setErrorMessage(conflictMsg.toString());
            return dto;
        }
        
        // Tạo suất chiếu mới
        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(dto.getStartTime());
        showtime.setEndTime(endTime);
        showtime.setPrice(dto.getPrice());
        showtime.setStatus(true);
        
        Showtime savedShowtime = showtimeRepository.save(showtime);
        
        dto.setId(savedShowtime.getId());
        dto.setMovieTitle(movie.getTitle());
        dto.setMovieDuration(movie.getDuration());
        dto.setRoomName(room.getName());
        dto.setHasConflict(false);
        dto.setErrorMessage(null);
        
        log.info("Tạo suất chiếu mới: {} - Phòng: {}, Thời gian: {}", movie.getTitle(), room.getName(), dto.getStartTime());
        
        return dto;
    }
    
    /**
     * Cập nhật suất chiếu với validation đầy đủ
     * 
     * Các bước kiểm tra (Validation):
     * 1. Kiểm tra suất chiếu có tồn tại không
     * 2. Kiểm tra phim có tồn tại không
     * 3. Kiểm tra phòng có tồn tại không
     * 4. Kiểm tra suất chiếu đã kết thúc chưa (không cho sửa nếu đã chiếu xong)
     * 5. Kiểm tra thời gian bắt đầu phải trước thời gian kết thúc
     * 6. Kiểm tra giá vé không được âm
     * 7. Kiểm tra xung đột thời gian với các suất chiếu khác trong cùng phòng
     * 8. Kiểm tra số vé đã bán để cảnh báo
     */
    @Transactional
    public ShowtimeDTO updateShowtime(ShowtimeDTO dto) {
        // ===== Bước 1: Kiểm tra suất chiếu tồn tại =====
        Showtime showtime = showtimeRepository.findById(dto.getId()).orElse(null);
        if (showtime == null) {
            dto.setErrorMessage("Suất chiếu không tồn tại hoặc đã bị xóa");
            dto.setHasConflict(false);
            return dto;
        }

        // Lưu thông tin hiện tại để phục hồi nếu cần
        dto.setCurrentStartTime(showtime.getStartTime());
        dto.setCurrentEndTime(showtime.getEndTime());
        dto.setCurrentMovieId(showtime.getMovie().getId());
        dto.setCurrentRoomId(showtime.getRoom().getId());
        dto.setCurrentPrice(showtime.getPrice());
        dto.setCurrentStatus(showtime.getStatus());
        dto.setCurrentMovieTitle(showtime.getMovie().getTitle());
        dto.setCurrentRoomName(showtime.getRoom().getName());

        // ===== Bước 2: Kiểm tra phim tồn tại =====
        Movie movie = movieRepository.findById(dto.getMovieId()).orElse(null);
        if (movie == null) {
            dto.setErrorMessage("Phim không tồn tại");
            dto.setHasConflict(false);
            return dto;
        }

        // ===== Bước 3: Kiểm tra phòng tồn tại =====
        Room room = roomRepository.findById(dto.getRoomId()).orElse(null);
        if (room == null) {
            dto.setErrorMessage("Phòng chiếu không tồn tại");
            dto.setHasConflict(false);
            return dto;
        }

        // ===== Bước 4: Kiểm tra suất chiếu đã kết thúc chưa =====
        if (showtime.getEndTime().isBefore(LocalDateTime.now())) {
            dto.setErrorMessage("Không thể chỉnh sửa suất chiếu đã kết thúc (kết thúc lúc: "
                    + showtime.getEndTime() + ")");
            dto.setHasConflict(false);
            return dto;
        }

        // ===== Bước 5: Kiểm tra thời gian =====
        if (dto.getStartTime() == null) {
            dto.setErrorMessage("Thời gian bắt đầu không được để trống");
            dto.setHasConflict(false);
            return dto;
        }

        // Tính toán endTime
        LocalDateTime endTime = calculateEndTime(dto.getStartTime(), movie.getDuration());
        dto.setEndTime(endTime);

        if (!endTime.isAfter(dto.getStartTime())) {
            dto.setErrorMessage("Thời gian kết thúc phải sau thời gian bắt đầu");
            dto.setHasConflict(false);
            return dto;
        }

        // ===== Bước 6: Kiểm tra giá vé =====
        if (dto.getPrice() != null && dto.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            dto.setErrorMessage("Giá vé không được âm");
            dto.setHasConflict(false);
            return dto;
        }

        // ===== Bước 7: Kiểm tra xung đột (loại trừ suất chiếu hiện tại) =====
        if (hasConflict(dto.getRoomId(), dto.getStartTime(), endTime, dto.getId())) {
            dto.setHasConflict(true);
            List<Showtime> conflicts = getConflictingShowtimes(dto.getRoomId(), dto.getStartTime(), endTime);
            conflicts = conflicts.stream()
                    .filter(s -> !s.getId().equals(dto.getId()))
                    .collect(Collectors.toList());

            StringBuilder conflictMsg = new StringBuilder("⚠️ Xung đột suất chiếu: Phòng " + room.getName() + " đã có suất chiếu vào:\n");
            for (Showtime conflict : conflicts) {
                conflictMsg.append("- ").append(conflict.getMovie().getTitle())
                        .append(" (").append(conflict.getStartTime()).append(" - ")
                        .append(conflict.getEndTime()).append(")\n");
            }
            dto.setErrorMessage(conflictMsg.toString());
            return dto;
        }

        // ===== Bước 8: Kiểm tra số vé đã bán =====
        int ticketCount = ticketRepository.countByShowtimeId(dto.getId());
        boolean hasPaid = ticketRepository.hasPaidBookings(dto.getId());
        dto.setTicketCount(ticketCount);
        dto.setHasPaidBookings(hasPaid);

        boolean hasTimeChange = !dto.getStartTime().equals(showtime.getStartTime());
        boolean hasRoomChange = !dto.getRoomId().equals(showtime.getRoom().getId());
        boolean hasMovieChange = !dto.getMovieId().equals(showtime.getMovie().getId());

        if (ticketCount > 0 && (hasTimeChange || hasRoomChange || hasMovieChange)) {
            log.warn("Cảnh báo: Suất chiếu {} đã có {} vé được đặt, đang thay đổi lịch chiếu", dto.getId(), ticketCount);
        }

        // ===== Tiến hành cập nhật =====
        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(dto.getStartTime());
        showtime.setEndTime(endTime);
        if (dto.getPrice() != null) {
            showtime.setPrice(dto.getPrice());
        }
        if (dto.getStatus() != null) {
            showtime.setStatus(dto.getStatus());
        }

        showtimeRepository.save(showtime);

        dto.setMovieTitle(movie.getTitle());
        dto.setMovieDuration(movie.getDuration());
        dto.setRoomName(room.getName());
        dto.setHasConflict(false);
        dto.setErrorMessage(null);

        log.info("Cập nhật suất chiếu thành công: {} - Phòng: {}, Thời gian: {} → {} ({} vé đã bán)",
                movie.getTitle(), room.getName(), showtime.getStartTime(), endTime, ticketCount);

        return dto;
    }
    
    /**
     * Xóa suất chiếu với kiểm tra vé đã đặt
     * 
     * Các bước kiểm tra (Validation):
     * 1. Kiểm tra suất chiếu có tồn tại không
     * 2. Kiểm tra số lượng vé đã đặt
     * 3. Nếu có vé PAID → không cho xóa cứng, chuyển sang vô hiệu hóa
     * 4. Nếu có vé PENDING → cho xóa kèm hủy vé
     * 5. Nếu không có vé → xóa hoàn toàn
     * 
     * @param id ID suất chiếu
     * @return DTO chứa kết quả và thông báo
     */
    @Transactional
    public ShowtimeDTO deleteShowtime(Integer id) {
        ShowtimeDTO dto = new ShowtimeDTO();
        dto.setId(id);

        // ===== Bước 1: Kiểm tra suất chiếu tồn tại =====
        Showtime showtime = showtimeRepository.findById(id).orElse(null);
        if (showtime == null) {
            dto.setErrorMessage("Suất chiếu không tồn tại hoặc đã bị xóa");
            dto.setHasConflict(false);
            return dto;
        }

        dto.setMovieTitle(showtime.getMovie().getTitle());
        dto.setRoomName(showtime.getRoom().getName());
        dto.setStartTime(showtime.getStartTime());

        // ===== Bước 2: Kiểm tra số lượng vé =====
        int ticketCount = ticketRepository.countByShowtimeId(id);
        boolean hasPaid = ticketRepository.hasPaidBookings(id);
        dto.setTicketCount(ticketCount);
        dto.setHasPaidBookings(hasPaid);

        // ===== Bước 3: Nếu có vé PAID → không cho xóa cứng =====
        if (hasPaid) {
            dto.setErrorMessage("Không thể xóa suất chiếu này vì đã có " + ticketCount
                    + " vé đã thanh toán. Hãy vô hiệu hóa (disable) suất chiếu thay vì xóa.");
            dto.setHasConflict(false);
            log.warn("Từ chối xóa suất chiếu {}: có {} vé đã thanh toán", id, ticketCount);
            return dto;
        }

        // ===== Bước 4: Nếu có vé PENDING → xóa với cảnh báo =====
        if (ticketCount > 0) {
            log.info("Xóa suất chiếu {} kèm {} vé PENDING/CANCELLED", id, ticketCount);
        }

        // ===== Bước 5: Tiến hành xóa =====
        showtimeRepository.deleteById(id);
        dto.setErrorMessage(null);
        dto.setHasConflict(false);

        log.info("Xóa suất chiếu thành công: {} - Phòng: {} ({} vé bị ảnh hưởng)", 
                showtime.getMovie().getTitle(), showtime.getRoom().getName(), ticketCount);

        return dto;
    }

    /**
     * Xóa suất chiếu (trả về void, chỉ xóa khi không có vé)
     * Dùng cho các trường hợp đã kiểm tra trước
     */
    @Transactional
    public void deleteShowtimeDirect(Integer id) {
        showtimeRepository.deleteById(id);
    }

    /**
     * Vô hiệu hóa suất chiếu (soft delete)
     * An toàn hơn xóa cứng, giữ nguyên dữ liệu vé đã đặt
     */
    @Transactional
    public void disableShowtime(Integer id) {
        Showtime showtime = showtimeRepository.findById(id).orElse(null);
        if (showtime != null) {
            int ticketCount = ticketRepository.countByShowtimeId(id);
            showtime.setStatus(false);
            showtimeRepository.save(showtime);
            log.info("Vô hiệu hóa suất chiếu: {} - Phòng: {} ({} vé bị ảnh hưởng)",
                    showtime.getMovie().getTitle(), showtime.getRoom().getName(), ticketCount);
        }
    }

    /**
     * Kích hoạt lại suất chiếu
     */
    @Transactional
    public void enableShowtime(Integer id) {
        Showtime showtime = showtimeRepository.findById(id).orElse(null);
        if (showtime != null) {
            showtime.setStatus(true);
            showtimeRepository.save(showtime);
            log.info("Kích hoạt suất chiếu: {} - Phòng: {}", showtime.getMovie().getTitle(), showtime.getRoom().getName());
        }
    }

    /**
     * Kiểm tra suất chiếu đã hết vé chưa
     * So sánh số vé đã bán (PAID) với tổng số ghế của phòng
     */
    public boolean isSoldOut(Integer showtimeId) {
        Showtime showtime = getShowtimeById(showtimeId);
        if (showtime == null || showtime.getRoom() == null || showtime.getRoom().getTotalSeats() == null) {
            return false;
        }
        int soldTickets = ticketRepository.countPaidByShowtimeId(showtimeId);
        return soldTickets >= showtime.getRoom().getTotalSeats();
    }

    /**
     * Lấy thông tin chi tiết suất chiếu kèm số vé đã bán
     * Dùng cho edit modal
     */
    public ShowtimeDTO getShowtimeDetail(Integer id) {
        Showtime showtime = showtimeRepository.findById(id).orElse(null);
        if (showtime == null) return null;

        ShowtimeDTO dto = new ShowtimeDTO();
        dto.setId(showtime.getId());
        dto.setMovieId(showtime.getMovie().getId());
        dto.setMovieTitle(showtime.getMovie().getTitle());
        dto.setMovieDuration(showtime.getMovie().getDuration());
        dto.setRoomId(showtime.getRoom().getId());
        dto.setRoomName(showtime.getRoom().getName());
        dto.setStartTime(showtime.getStartTime());
        dto.setEndTime(showtime.getEndTime());
        dto.setPrice(showtime.getPrice());
        dto.setStatus(showtime.getStatus());

        dto.setTicketCount(ticketRepository.countByShowtimeId(id));
        dto.setHasPaidBookings(ticketRepository.hasPaidBookings(id));

        return dto;
    }
}
