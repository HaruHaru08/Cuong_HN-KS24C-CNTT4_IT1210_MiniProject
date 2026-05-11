package com.example.smart_cinema_booking_system.controller;

import com.example.smart_cinema_booking_system.dto.BookingRequest;
import com.example.smart_cinema_booking_system.dto.BookingResponse;
import com.example.smart_cinema_booking_system.dto.SeatDTO;
import com.example.smart_cinema_booking_system.model.*;
import com.example.smart_cinema_booking_system.repository.BookingRepository;
import com.example.smart_cinema_booking_system.repository.SeatRepository;
import com.example.smart_cinema_booking_system.repository.TicketRepository;
import com.example.smart_cinema_booking_system.service.BookingService;
import com.example.smart_cinema_booking_system.service.ShowtimeService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.example.smart_cinema_booking_system.exception.BusinessException;

@Controller
@RequestMapping("/booking")
@RequiredArgsConstructor
@Slf4j
public class BookingController {
    
    private final BookingService bookingService;
    private final ShowtimeService showtimeService;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    @GetMapping("/showtime/{showtimeId}")
    public String showBookingPage(@PathVariable Integer showtimeId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Showtime showtime = showtimeService.getShowtimeById(showtimeId);
        if (showtime == null) return "redirect:/customer/home";

        // Guard: không cho truy cập suất chiếu đã qua (CORE-08)
        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            return "redirect:/customer/home?error=expired";
        }

        List<SeatDTO> seats = bookingService.getSeatsForShowtime(showtimeId);

        Map<String, List<SeatDTO>> seatsByRow = seats.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getSeatNumber().substring(0, 1),
                        TreeMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("seatsByRow", seatsByRow);
        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("showtime", showtime);

        return "booking/seat-map";
    }

    @PostMapping("/confirm")
    public String confirmBooking(@RequestParam("showtimeId") Integer showtimeId,
                                  @RequestParam("selectedSeatIds") List<Integer> selectedSeatIds,
                                  HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        if (selectedSeatIds == null || selectedSeatIds.isEmpty()) {
            return "redirect:/booking/showtime/" + showtimeId;
        }

        Showtime showtime = showtimeService.getShowtimeById(showtimeId);
        if (showtime == null) return "redirect:/customer/home";

        List<Integer> bookedIds = ticketRepository.findActiveByShowtimeId(showtimeId)
                .stream().map(t -> t.getSeat().getId()).collect(Collectors.toList());

        List<Integer> conflicts = selectedSeatIds.stream()
                .filter(bookedIds::contains).collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            return "redirect:/booking/showtime/" + showtimeId + "?error=conflict";
        }

        List<Seat> seats = seatRepository.findAllById(selectedSeatIds);

        BigDecimal total = BigDecimal.ZERO;
        List<Map<String, Object>> seatDetails = new ArrayList<>();

        for (Seat seat : seats) {
            BigDecimal price = showtime.getPrice().add(seat.getSeatType().getSurcharge());
            total = total.add(price);

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("seatNumber", seat.getSeatNumber());
            detail.put("typeName", seat.getSeatType().getTypeName());
            detail.put("surcharge", seat.getSeatType().getSurcharge());
            detail.put("price", price);
            seatDetails.add(detail);
        }

        model.addAttribute("showtime", showtime);
        model.addAttribute("showtimeId", showtimeId);
        model.addAttribute("seatDetails", seatDetails);
        model.addAttribute("totalAmount", total);
        model.addAttribute("selectedSeatIds", selectedSeatIds);
        model.addAttribute("seatCount", seats.size());

        return "booking/invoice";
    }

    @PostMapping("/pay")
    @Transactional
    public String payBooking(@RequestParam("showtimeId") Integer showtimeId,
                              @RequestParam("selectedSeatIds") List<Integer> selectedSeatIds,
                              HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        if (selectedSeatIds == null || selectedSeatIds.isEmpty()) {
            return "redirect:/booking/showtime/" + showtimeId;
        }

        Showtime showtime = showtimeService.getShowtimeById(showtimeId);
        if (showtime == null) return "redirect:/customer/home";

        List<Integer> alreadyBookedIds = ticketRepository.findActiveByShowtimeId(showtimeId)
                .stream().map(t -> t.getSeat().getId()).collect(Collectors.toList());

        List<Integer> conflicts = selectedSeatIds.stream()
                .filter(alreadyBookedIds::contains).collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            return "redirect:/booking/showtime/" + showtimeId + "?error=conflict";
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStatus(BookingStatus.PAID);
        booking.setBookingDate(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        List<Ticket> tickets = new ArrayList<>();

        List<Seat> seats = seatRepository.findAllByIdWithLock(selectedSeatIds);
        Map<Integer, Seat> seatMap = seats.stream()
                .collect(Collectors.toMap(Seat::getId, s -> s));

        for (Integer seatId : selectedSeatIds) {
            Seat seat = seatMap.get(seatId);
            if (seat == null) continue;

            BigDecimal price = showtime.getPrice().add(seat.getSeatType().getSurcharge());
            total = total.add(price);

            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setShowtime(showtime);
            ticket.setSeat(seat);
            ticket.setPrice(price);
            tickets.add(ticket);
        }

        booking.setTotalAmount(total);
        booking = bookingRepository.save(booking);
        ticketRepository.saveAll(tickets);

        return "redirect:/booking/confirmation/" + booking.getId();
    }

    @GetMapping("/confirmation/{bookingId}")
    public String showConfirmation(@PathVariable Integer bookingId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Booking booking = bookingService.getBookingDetail(bookingId);
        if (booking == null || !booking.getUser().getId().equals(user.getId())) {
            return "redirect:/customer/home";
        }

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        model.addAttribute("booking", booking);
        model.addAttribute("tickets", tickets);
        if (!tickets.isEmpty()) model.addAttribute("showtime", tickets.get(0).getShowtime());

        return "booking/confirmation";
    }

    @PostMapping("/api/process")
    @ResponseBody
    public ResponseEntity<BookingResponse> processBooking(@RequestBody BookingRequest request, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return ResponseEntity.status(401).build();
        
        return ResponseEntity.ok(bookingService.processBooking(request, user.getId()));
    }

    /**
     * API hủy vé (cho my-bookings.html gọi)
     * Trả về JSON { "success": true/false, "message": "..." }
     */
    @PostMapping("/api/cancel/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelBookingApi(@PathVariable Integer id, HttpSession session) {
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
    
    @GetMapping("/my-bookings")
    public String showMyBookings(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";
        
        model.addAttribute("history", bookingService.getBookingHistory(user.getId()));
        return "booking/history"; // Chuyển sang dùng chung view history
    }

    @GetMapping("/ticket/{bookingId}")
    public String showTicketDetail(@PathVariable Integer bookingId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/auth/login";

        Booking booking = bookingService.getBookingDetail(bookingId);
        if (!booking.getUser().getId().equals(user.getId())) return "redirect:/";

        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        model.addAttribute("booking", booking);
        model.addAttribute("tickets", tickets);
        if (!tickets.isEmpty()) model.addAttribute("showtime", tickets.get(0).getShowtime());

        return "booking/detail";
    }
}
