package com.example.smart_cinema_booking_system.service;

import com.example.smart_cinema_booking_system.dto.*;
import com.example.smart_cinema_booking_system.exception.BusinessException;
import com.example.smart_cinema_booking_system.model.*;
import com.example.smart_cinema_booking_system.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TicketRepository ticketRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public List<SeatDTO> getSeatsForShowtime(Integer showtimeId) {
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy suất chiếu"));
        
        List<Seat> allSeats = seatRepository.findByRoomId(showtime.getRoom().getId());
        
        // Chỉ lấy vé từ booking không bị hủy (CANCELLED), để ghế đã hủy hiển thị lại là trống
        List<Integer> bookedSeatIds = ticketRepository.findActiveByShowtimeId(showtimeId)
                .stream().map(t -> t.getSeat().getId()).collect(Collectors.toList());

        return allSeats.stream().map(seat -> {
            boolean isBooked = bookedSeatIds.contains(seat.getId());
            return new SeatDTO(
                seat.getId(),
                seat.getSeatNumber(),
                seat.getSeatType().getTypeName(),
                showtime.getPrice().doubleValue() + seat.getSeatType().getSurcharge().doubleValue(),
                isBooked,
                isBooked ? "booked" : "available"
            );
        }).collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse processBooking(BookingRequest request, Integer userId) {
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new RuntimeException("Suất chiếu không tồn tại"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        List<Integer> alreadyBookedSeatIds = ticketRepository.findActiveByShowtimeId(request.getShowtimeId())
                .stream().map(t -> t.getSeat().getId()).collect(Collectors.toList());

        List<Integer> conflicts = request.getSeatIds().stream()
                .filter(alreadyBookedSeatIds::contains).collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            return new BookingResponse(false, "Ghế đã có người đặt", null, null, null);
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setStatus(BookingStatus.PAID);
        booking.setBookingDate(LocalDateTime.now());
        
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Ticket> tickets = new ArrayList<>();

        for (Integer seatId : request.getSeatIds()) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new RuntimeException("Ghế không tồn tại"));
            
            // Sử dụng seat.getSeatType()
            BigDecimal ticketPrice = showtime.getPrice().add(seat.getSeatType().getSurcharge());
            totalAmount = totalAmount.add(ticketPrice);

            Ticket ticket = new Ticket();
            ticket.setBooking(booking);
            ticket.setShowtime(showtime);
            ticket.setSeat(seat);
            ticket.setPrice(ticketPrice);
            tickets.add(ticket);
        }

        booking.setTotalAmount(totalAmount);
        Booking savedBooking = bookingRepository.save(booking);
        ticketRepository.saveAll(tickets);

        return new BookingResponse(true, "Thành công", savedBooking.getId(), totalAmount.doubleValue(), null);
    }

    /**
     * Lấy danh sách thô (Object[]) để khớp với controller hiện tại
     */
    public List<Object[]> getBookingHistory(Integer userId) {
        return bookingRepository.findBookingHistoryByUserId(userId);
    }

    public Optional<Booking> getBookingById(Integer id) {
        return bookingRepository.findById(id);
    }

    public List<Ticket> getTicketsByBookingId(Integer bookingId) {
        return ticketRepository.findByBookingId(bookingId);
    }

    public Booking getBookingDetail(Integer bookingId) {
        return bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));
    }

    @Transactional
    public String cancelBooking(Integer bookingId, Integer userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Không có quyền hủy vé này");
        }

        // Kiểm tra trạng thái: chỉ cho hủy vé đã thanh toán
        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BusinessException("Chỉ có thể hủy vé đã thanh toán");
        }

        // Lấy suất chiếu từ vé đầu tiên
        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);
        if (tickets.isEmpty()) {
            throw new BusinessException("Không tìm thấy vé cho booking này");
        }
        Showtime showtime = tickets.get(0).getShowtime();

        // Kiểm tra ràng buộc thời gian: phải hủy trước 24h so với giờ chiếu
        long hoursUntilShow = ChronoUnit.HOURS.between(LocalDateTime.now(), showtime.getStartTime());
        if (hoursUntilShow < 24) {
            throw new BusinessException(
                "Chỉ có thể hủy vé trước 24 giờ so với giờ chiếu. "
                + "Suất chiếu còn " + hoursUntilShow + " giờ nữa."
            );
        }

        // Soft delete: cập nhật trạng thái CANCELLED, không xóa vé hay booking
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        log.info("Hủy vé thành công: bookingId={}, userId={}, showtimeId={}", bookingId, userId, showtime.getId());
        return "Hủy thành công";
    }
}
