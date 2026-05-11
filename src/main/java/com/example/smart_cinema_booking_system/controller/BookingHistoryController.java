package com.example.smart_cinema_booking_system.controller;

import com.example.smart_cinema_booking_system.exception.BusinessException;
import com.example.smart_cinema_booking_system.model.Booking;
import com.example.smart_cinema_booking_system.model.Ticket;
import com.example.smart_cinema_booking_system.model.User;
import com.example.smart_cinema_booking_system.service.BookingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingHistoryController {

    private final BookingService bookingService;

    /**
     * Hiển thị lịch sử đặt vé
     */
    @GetMapping("/history")
    public String showHistory(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        List<Object[]> history = bookingService.getBookingHistory(user.getId());
        model.addAttribute("history", history);
        return "booking/history";
    }

    /**
     * Hiển thị chi tiết vé (Dạng Boarding Pass)
     */
    @GetMapping("/detail/{id}")
    public String showDetail(@PathVariable Integer id, HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Booking booking = bookingService.getBookingById(id).orElse(null);
        if (booking == null || !booking.getUser().getId().equals(user.getId())) {
            return "redirect:/bookings/history";
        }

        List<Ticket> tickets = bookingService.getTicketsByBookingId(id);
        model.addAttribute("booking", booking);
        model.addAttribute("tickets", tickets);
        // Lấy thông tin chung từ vé đầu tiên
        if (!tickets.isEmpty()) {
            model.addAttribute("showtime", tickets.get(0).getShowtime());
        }

        return "booking/detail";
    }

    /**
     * API Hủy vé
     */
    @PostMapping("/cancel/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelBooking(@PathVariable Integer id, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Vui lòng đăng nhập"));
        }

        try {
            String result = bookingService.cancelBooking(id, user.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", result));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}
