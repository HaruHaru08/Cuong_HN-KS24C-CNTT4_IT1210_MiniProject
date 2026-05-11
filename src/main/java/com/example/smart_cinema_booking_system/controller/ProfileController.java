package com.example.smart_cinema_booking_system.controller;

import com.example.smart_cinema_booking_system.dto.ProfileDTO;
import com.example.smart_cinema_booking_system.model.User;
import com.example.smart_cinema_booking_system.model.UserProfile;
import com.example.smart_cinema_booking_system.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public String viewProfile(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/auth/login";
        }

        UserProfile profile = profileService.getProfileByUserId(loggedInUser.getId());
        model.addAttribute("profile", profile);
        return "profile/view";
    }

    @GetMapping("/edit")
    public String showEditForm(HttpSession session, Model model) {
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/auth/login";
        }

        UserProfile profile = profileService.getProfileByUserId(loggedInUser.getId());
        ProfileDTO profileDTO = new ProfileDTO();
        profileDTO.setUserId(loggedInUser.getId());
        if (profile != null) {
            profileDTO.setFullName(profile.getFullName());
            profileDTO.setPhone(profile.getPhone());
            profileDTO.setAddress(profile.getAddress());
        }

        model.addAttribute("profileDTO", profileDTO);
        return "profile/edit";
    }

    @PostMapping("/save")
    public String saveProfile(@Valid @ModelAttribute("profileDTO") ProfileDTO profileDTO,
                               BindingResult result,
                               HttpSession session,
                               RedirectAttributes ra) {
        
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        if (loggedInUser == null) {
            return "redirect:/auth/login";
        }

        if (result.hasErrors()) {
            return "profile/edit";
        }

        profileDTO.setUserId(loggedInUser.getId());
        profileService.updateProfile(profileDTO);
        
        // Cập nhật lại thông tin fullName trong Session để hiển thị đúng trên Navbar
        loggedInUser.setFullName(profileDTO.getFullName());
        session.setAttribute("loggedInUser", loggedInUser);

        ra.addFlashAttribute("message", "Cập nhật hồ sơ thành công!");
        return "redirect:/profile";
    }
}
