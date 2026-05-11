package com.example.smart_cinema_booking_system.controller;

import com.example.smart_cinema_booking_system.dto.ShowtimeDTO;
import com.example.smart_cinema_booking_system.model.Movie;
import com.example.smart_cinema_booking_system.model.Room;
import com.example.smart_cinema_booking_system.model.Showtime;
import com.example.smart_cinema_booking_system.model.User;
import com.example.smart_cinema_booking_system.model.UserRole;
import com.example.smart_cinema_booking_system.service.MovieService;
import com.example.smart_cinema_booking_system.service.RoomService;
import com.example.smart_cinema_booking_system.service.SeatService;
import com.example.smart_cinema_booking_system.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Controller cho quản lý suất chiếu
 */
@Controller
@RequestMapping("/admin/showtimes")
@RequiredArgsConstructor
@Slf4j
public class ShowtimeController {
    
    private final ShowtimeService showtimeService;
    private final MovieService movieService;
    private final RoomService roomService;
    private final SeatService seatService;
    
    /**
     * Hiển thị trang quản lý suất chiếu
     */
    @GetMapping
    public String showShowtimeManagement(
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "room", required = false) Integer roomId,
            Model model,
            HttpSession session) {
        
        // Kiểm tra quyền admin
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/auth/login";
        }
        
        // Xác định ngày hiển thị (mặc định là hôm nay)
        LocalDate selectedDate = dateStr != null && !dateStr.isEmpty() 
                ? LocalDate.parse(dateStr) 
                : LocalDate.now();
        
        // Lấy danh sách phòng
        List<Room> rooms = roomService.getAllRooms();
        model.addAttribute("rooms", rooms);
        
        // Nếu có chọn phòng cụ thể
        List<Showtime> showtimes;
        if (roomId != null) {
            showtimes = showtimeService.getShowtimesByRoomAndDate(roomId, selectedDate);
            Room selectedRoom = roomService.getRoomById(roomId);
            model.addAttribute("selectedRoom", selectedRoom);
        } else {
            // Lấy tất cả suất chiếu của ngày đó
            showtimes = showtimeService.getShowtimesByDate(selectedDate);
        }
        
        model.addAttribute("showtimes", showtimes);
        model.addAttribute("selectedDate", selectedDate);
        model.addAttribute("selectedRoomId", roomId);
        model.addAttribute("currentMonth", YearMonth.now());
        
        // Khởi tạo DTO mới để hiển thị form
        ShowtimeDTO newShowtime = new ShowtimeDTO();
        model.addAttribute("showtimeForm", newShowtime);
        
        // Lấy danh sách phim
        List<Movie> movies = movieService.getAllMovies();
        model.addAttribute("movies", movies);
        
        // --- Dữ liệu cho Schedule Board ---
        // Nhóm suất chiếu theo phòng, sắp xếp theo ID phòng
        Map<Room, List<Showtime>> showtimesByRoom = showtimes.stream()
                .collect(Collectors.groupingBy(Showtime::getRoom,
                        () -> new TreeMap<>(Comparator.comparing(Room::getId)),
                        Collectors.toList()));
        model.addAttribute("showtimesByRoom", showtimesByRoom);
        
        // Thống kê nhanh
        long todayShowtimeCount = showtimes.size();
        long activeRoomsCount = showtimes.stream()
                .map(Showtime::getRoom)
                .distinct()
                .count();
        int totalSeats = rooms.stream()
                .mapToInt(Room::getTotalSeats)
                .sum();
        
        model.addAttribute("todayShowtimeCount", todayShowtimeCount);
        model.addAttribute("activeRoomsCount", activeRoomsCount);
        model.addAttribute("totalSeats", totalSeats);
        // MOCK: bookedSeatsCount — cần tích hợp query từ bảng tickets
        model.addAttribute("bookedSeatsCount", 0);
        // Thời gian hiện tại để xác định trạng thái suất chiếu (đang/sắp/đã chiếu)
        model.addAttribute("currentTime", java.time.LocalDateTime.now());
        // Danh sách giờ cho time ruler (06:00 → 23:00)
        model.addAttribute("hours", java.util.stream.IntStream.rangeClosed(6, 23).boxed().collect(Collectors.toList()));
        
        log.debug("=== SHOWTIME DEBUG ===");
        log.debug("selectedDate: {}", selectedDate);
        log.debug("showtimes count: {}", showtimes.size());
        log.debug("showtimesByRoom size: {}", showtimesByRoom.size());
        log.debug("rooms count: {}", rooms.size());
        log.debug("todayShowtimeCount: {}", todayShowtimeCount);
        log.debug("activeRoomsCount: {}", activeRoomsCount);
        log.debug("totalSeats: {}", totalSeats);
        
        return "admin/showtimes/index";
    }
    
    /**
     * Hiển thị timeline/calendar view
     */
    @GetMapping("/calendar")
    public String showCalendar(
            @RequestParam(value = "month", required = false) String monthStr,
            Model model,
            HttpSession session) {
        
        // Kiểm tra quyền admin
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/auth/login";
        }
        
        // Xác định tháng hiển thị
        YearMonth selectedMonth = monthStr != null && !monthStr.isEmpty()
                ? YearMonth.parse(monthStr)
                : YearMonth.now();
        
        // Lấy danh sách phòng
        List<Room> rooms = roomService.getAllRooms();
        model.addAttribute("rooms", rooms);
        
        // Lấy các suất chiếu theo tháng
        var showtimesByRoom = showtimeService.getShowtimesByMonth(selectedMonth);
        model.addAttribute("showtimesByRoom", showtimesByRoom);
        model.addAttribute("selectedMonth", selectedMonth);
        
        return "admin/showtimes/calendar";
    }
    
    /**
     * Tạo ghế cho tất cả phòng (chạy thủ công — xóa cũ, tạo mới)
     */
    @PostMapping("/generate-seats")
    public String generateSeats(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/auth/login";
        }

        try {
            seatService.initSeatTypes();
            seatService.regenerateAllRoomSeats();
            model.addAttribute("success", "Đã tạo lại ghế thành công cho tất cả phòng!");
        } catch (Exception e) {
            log.error("Lỗi khi tạo ghế: {}", e.getMessage(), e);
            model.addAttribute("error", "Lỗi khi tạo ghế: " + e.getMessage());
        }

        return "redirect:/admin/showtimes";
    }

    /**
     * Tạo lại ghế cho một phòng cụ thể (chạy thủ công)
     */
    @PostMapping("/generate-seats/{roomId}")
    public String generateSeatsForRoom(
            @PathVariable Integer roomId,
            HttpSession session,
            Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/auth/login";
        }

        try {
            boolean success = seatService.regenerateRoomSeats(roomId);
            if (success) {
                model.addAttribute("success", "Đã tạo lại ghế cho phòng ID=" + roomId);
            } else {
                model.addAttribute("error", "Không tìm thấy phòng ID=" + roomId);
            }
        } catch (Exception e) {
            log.error("Lỗi khi tạo ghế cho phòng {}: {}", roomId, e.getMessage(), e);
            model.addAttribute("error", "Lỗi: " + e.getMessage());
        }

        return "redirect:/admin/showtimes";
    }

    /**
     * API: Lấy suất chiếu cho AJAX
     */
    @GetMapping("/api/list")
    @ResponseBody
    public List<Showtime> getShowtimes(
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "room", required = false) Integer roomId) {
        
        LocalDate selectedDate = dateStr != null && !dateStr.isEmpty()
                ? LocalDate.parse(dateStr)
                : LocalDate.now();
        
        if (roomId != null) {
            return showtimeService.getShowtimesByRoomAndDate(roomId, selectedDate);
        }
        return showtimeService.getShowtimesByDate(selectedDate);
    }
    
    /**
     * API: Kiểm tra xung đột (AJAX)
     */
    @PostMapping("/api/check-conflict")
    @ResponseBody
    public ConflictCheckResponse checkConflict(
            @RequestParam Integer roomId,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) Integer excludeShowtimeId) {
        
        var startDateTime = java.time.LocalDateTime.parse(startTime);
        var endDateTime = java.time.LocalDateTime.parse(endTime);
        
        boolean hasConflict = showtimeService.hasConflict(roomId, startDateTime, endDateTime, excludeShowtimeId);
        
        ConflictCheckResponse response = new ConflictCheckResponse();
        response.setHasConflict(hasConflict);
        
        if (hasConflict) {
            List<Showtime> conflicts = showtimeService.getConflictingShowtimes(roomId, startDateTime, endDateTime);
            response.setConflictingShowtimes(conflicts);
        }
        
        return response;
    }
    
    /**
     * API: Lấy thông tin suất chiếu cho edit modal (AJAX)
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ShowtimeDTO getShowtimeDetail(@PathVariable Integer id) {
        ShowtimeDTO dto = showtimeService.getShowtimeDetail(id);
        if (dto == null) {
            dto = new ShowtimeDTO();
            dto.setErrorMessage("Không tìm thấy suất chiếu");
        }
        return dto;
    }

    /**
     * Tạo suất chiếu mới (POST)
     */
    @PostMapping
    public String createShowtime(
            @ModelAttribute ShowtimeDTO showtimeDTO,
            Model model,
            HttpSession session,
            @RequestParam(value = "date", required = false) String dateStr) {
        
        // Kiểm tra quyền admin
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/auth/login";
        }
        
        // Log để debug
        log.info("createShowtime called with: movieId={}, roomId={}, startTime={}, price={}", 
                showtimeDTO.getMovieId(), showtimeDTO.getRoomId(), showtimeDTO.getStartTime(), showtimeDTO.getPrice());
        
        // Tạo suất chiếu
        ShowtimeDTO result = showtimeService.createShowtime(showtimeDTO);
        
        // Nếu có lỗi, quay lại với thông báo lỗi
        if (result.isHasConflict() || result.getErrorMessage() != null) {
            log.warn("Lỗi khi tạo suất chiếu: {}", result.getErrorMessage());
            model.addAttribute("error", result.getErrorMessage());
            return "redirect:/admin/showtimes" + (dateStr != null ? "?date=" + dateStr : "");
        }
        
        log.info("Tạo suất chiếu thành công: {}", result.getId());
        return "redirect:/admin/showtimes" + (dateStr != null ? "?date=" + dateStr : "");
    }
    
    /**
     * Cập nhật suất chiếu (POST)
     */
    @PostMapping("/{id}/update")
    public String updateShowtime(
            @PathVariable Integer id,
            @ModelAttribute ShowtimeDTO showtimeDTO,
            Model model,
            HttpSession session,
            @RequestParam(value = "date", required = false) String dateStr) {
        
        // Kiểm tra quyền admin
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/auth/login";
        }
        
        showtimeDTO.setId(id);
        
        // Cập nhật suất chiếu
        ShowtimeDTO result = showtimeService.updateShowtime(showtimeDTO);
        
        // Nếu có lỗi, quay lại form
        if (result.isHasConflict() || result.getErrorMessage() != null) {
            model.addAttribute("error", result.getErrorMessage());
            return "redirect:/admin/showtimes" + (dateStr != null ? "?date=" + dateStr : "");
        }
        
        log.info("Cập nhật suất chiếu thành công: {}", id);
        return "redirect:/admin/showtimes" + (dateStr != null ? "?date=" + dateStr : "");
    }
    
    /**
     * Xóa suất chiếu với validation
     * 
     * Các bước kiểm tra:
     * 1. Kiểm tra quyền admin
     * 2. Kiểm tra suất chiếu có tồn tại không
     * 3. Kiểm tra có vé PAID không → không cho xóa, chuyển disable
     * 4. Nếu có vé PENDING → xóa kèm thông báo
     * 5. Nếu không có vé → xóa hoàn toàn
     */
    @PostMapping("/{id}/delete")
    public String deleteShowtime(
            @PathVariable Integer id,
            HttpSession session,
            Model model,
            @RequestParam(value = "date", required = false) String dateStr) {
        
        // Bước 1: Kiểm tra quyền admin
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/auth/login";
        }
        
        // Bước 2-5: Xóa với validation
        ShowtimeDTO result = showtimeService.deleteShowtime(id);
        
        if (result.getErrorMessage() != null) {
            // Không thể xóa (có vé PAID) → chuyển hướng với thông báo lỗi
            model.addAttribute("error", result.getErrorMessage());
            log.warn("Không thể xóa suất chiếu {}: {}", id, result.getErrorMessage());
        } else {
            if (result.getTicketCount() > 0) {
                log.info("Đã xóa suất chiếu {} kèm {} vé PENDING/CANCELLED", id, result.getTicketCount());
            } else {
                log.info("Xóa suất chiếu thành công: {}", id);
            }
        }
        
        return "redirect:/admin/showtimes" + (dateStr != null ? "?date=" + dateStr : "");
    }
    
    /**
     * Vô hiệu hóa suất chiếu
     */
    @PostMapping("/{id}/disable")
    public String disableShowtime(
            @PathVariable Integer id,
            HttpSession session,
            @RequestParam(value = "date", required = false) String dateStr) {
        
        // Kiểm tra quyền admin
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/auth/login";
        }
        
        showtimeService.disableShowtime(id);
        log.info("Vô hiệu hóa suất chiếu thành công: {}", id);
        
        return "redirect:/admin/showtimes" + (dateStr != null ? "?date=" + dateStr : "");
    }
    
    /**
     * Kích hoạt suất chiếu
     */
    @PostMapping("/{id}/enable")
    public String enableShowtime(
            @PathVariable Integer id,
            HttpSession session,
            @RequestParam(value = "date", required = false) String dateStr) {
        
        // Kiểm tra quyền admin
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.ADMIN) {
            return "redirect:/auth/login";
        }
        
        showtimeService.enableShowtime(id);
        log.info("Kích hoạt suất chiếu thành công: {}", id);
        
        return "redirect:/admin/showtimes" + (dateStr != null ? "?date=" + dateStr : "");
    }
    
    /**
     * Response class cho AJAX check conflict
     */
    public static class ConflictCheckResponse {
        private boolean hasConflict;
        private List<Showtime> conflictingShowtimes;
        
        public boolean isHasConflict() {
            return hasConflict;
        }
        
        public void setHasConflict(boolean hasConflict) {
            this.hasConflict = hasConflict;
        }
        
        public List<Showtime> getConflictingShowtimes() {
            return conflictingShowtimes;
        }
        
        public void setConflictingShowtimes(List<Showtime> conflictingShowtimes) {
            this.conflictingShowtimes = conflictingShowtimes;
        }
    }
}
