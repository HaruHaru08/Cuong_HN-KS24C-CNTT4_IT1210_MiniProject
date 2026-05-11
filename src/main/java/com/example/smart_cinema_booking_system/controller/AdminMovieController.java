package com.example.smart_cinema_booking_system.controller;

import com.example.smart_cinema_booking_system.model.Movie;
import com.example.smart_cinema_booking_system.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/admin/movies")
public class AdminMovieController {

    @Autowired
    private MovieService movieService;

    @GetMapping
    public String listMovies(Model model) {
        model.addAttribute("movies", movieService.getAllMovies());
        return "admin/movies/list";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("movie", new Movie());
        return "admin/movies/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Integer id, Model model) {
        Movie movie = movieService.getMovieById(id);
        if (movie == null) {
            return "redirect:/admin/movies";
        }
        model.addAttribute("movie", movie);
        return "admin/movies/form";
    }

    @PostMapping("/save")
    public String saveMovie(@ModelAttribute("movie") Movie movie,
                            @RequestParam(value = "file", required = false) MultipartFile file,
                            RedirectAttributes ra) {
        try {
            movieService.saveMovie(movie, file);
            ra.addFlashAttribute("message", "Luu phim thanh cong!");
        } catch (IOException e) {
            ra.addFlashAttribute("error", "Loi upload anh: " + e.getMessage());
        }
        return "redirect:/admin/movies";
    }

    @GetMapping("/delete/{id}")
    public String deleteMovie(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            movieService.deleteMovie(id);
            ra.addFlashAttribute("message", "Xoa phim thanh cong!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Khong the xoa phim nay!");
        }
        return "redirect:/admin/movies";
    }
}
