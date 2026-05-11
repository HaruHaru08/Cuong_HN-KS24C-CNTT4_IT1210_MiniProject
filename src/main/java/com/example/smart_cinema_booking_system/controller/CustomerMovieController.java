package com.example.smart_cinema_booking_system.controller;

import com.example.smart_cinema_booking_system.model.Movie;
import com.example.smart_cinema_booking_system.model.Showtime;
import com.example.smart_cinema_booking_system.model.User;
import com.example.smart_cinema_booking_system.model.UserRole;
import com.example.smart_cinema_booking_system.service.MovieService;
import com.example.smart_cinema_booking_system.service.ShowtimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/customer/movies")
@RequiredArgsConstructor
public class CustomerMovieController {

    private final MovieService movieService;
    private final ShowtimeService showtimeService;

    @GetMapping("/{movieId}")
    public String showMovieDetail(@PathVariable Integer movieId, Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.CUSTOMER) {
            return "redirect:/auth/login";
        }

        Movie movie = movieService.getMovieById(movieId);
        if (movie == null) {
            return "redirect:/customer/home";
        }

        List<Showtime> allShowtimes = showtimeService.getShowtimesByMovie(movieId);
        List<Showtime> availableShowtimes = allShowtimes.stream()
                .filter(s -> s.getStatus() != null && s.getStatus()
                        && s.getStartTime() != null && s.getStartTime().isAfter(LocalDateTime.now()))
                .sorted((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .collect(Collectors.toList());

        // Xác định suất chiếu đã hết vé (CORE-08)
        Set<Integer> soldOutShowtimeIds = availableShowtimes.stream()
                .filter(s -> showtimeService.isSoldOut(s.getId()))
                .map(Showtime::getId)
                .collect(Collectors.toSet());

        model.addAttribute("movie", movie);
        model.addAttribute("showtimes", availableShowtimes);
        model.addAttribute("soldOutShowtimeIds", soldOutShowtimeIds);

        return "customer/movie-detail";
    }
}
