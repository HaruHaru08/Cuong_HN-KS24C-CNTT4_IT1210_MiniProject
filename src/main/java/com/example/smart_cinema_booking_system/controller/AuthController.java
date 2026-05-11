package com.example.smart_cinema_booking_system.controller;

import com.example.smart_cinema_booking_system.dto.UserDTO;
import com.example.smart_cinema_booking_system.model.User;
import com.example.smart_cinema_booking_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "auth/Login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new UserDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String registerUser(UserDTO userDTO) {
        userService.registerUser(userDTO);
        return "redirect:/auth/login";
    }

    @PostMapping("/login")
    public String loginUser(String username, String password, HttpSession session) {
        log.info("Login attempt for username: {}", username);
        User user = userService.login(username, password);
        if (user != null) {
            log.info("Login successful for user: {} with role: {}", user.getUserName(), user.getRole());
            session.setAttribute("loggedInUser", user);
            // Redirect directly to role-specific home page
            switch (user.getRole()) {
                case ADMIN:
                    return "redirect:/admin/home";
                case STAFF:
                    return "redirect:/staff/home";
                case CUSTOMER:
                default:
                    return "redirect:/customer/home";
            }
        }
        log.warn("Login failed for username: {}", username);
        return "redirect:/auth/login?error=true";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login";
    }
}