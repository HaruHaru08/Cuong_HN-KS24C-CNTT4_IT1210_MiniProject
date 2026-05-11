package com.example.smart_cinema_booking_system.controller;

import com.example.smart_cinema_booking_system.model.Movie;
import com.example.smart_cinema_booking_system.model.User;
import com.example.smart_cinema_booking_system.model.UserRole;
import com.example.smart_cinema_booking_system.service.MovieService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class HomeController {

    private final MovieService movieService;

    public HomeController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");

        if (user == null) {
            return "redirect:/auth/login";
        }

        if (user.getRole() == UserRole.ADMIN) {
            return "redirect:/admin/home";
        } else if (user.getRole() == UserRole.STAFF) {
            return "redirect:/staff/home";
        } else {
            return "redirect:/customer/home";
        }
    }

    @GetMapping("/staff/home")
    public String staffHome(HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.STAFF) {
            return "redirect:/auth/login";
        }
        return "staff/home";
    }

    @GetMapping("/customer/home")
    public String customerHome(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null || user.getRole() != UserRole.CUSTOMER) {
            return "redirect:/auth/login";
        }

        List<Movie> allMovies = movieService.getAllMovies();
        LocalDate today = LocalDate.now();

        List<Movie> nowPlaying = allMovies.stream()
                .filter(m -> m.getReleaseDate() != null && !m.getReleaseDate().isAfter(today))
                .collect(Collectors.toList());

        List<Movie> upcoming = allMovies.stream()
                .filter(m -> m.getReleaseDate() == null || m.getReleaseDate().isAfter(today))
                .collect(Collectors.toList());

        List<Movie> featuredMovies = nowPlaying.isEmpty()
                ? allMovies.stream().limit(5).collect(Collectors.toList())
                : nowPlaying.stream().limit(5).collect(Collectors.toList());

        model.addAttribute("nowPlaying", nowPlaying);
        model.addAttribute("upcoming", upcoming);
        model.addAttribute("featuredMovies", featuredMovies);

        return "customer/home";
    }
}
