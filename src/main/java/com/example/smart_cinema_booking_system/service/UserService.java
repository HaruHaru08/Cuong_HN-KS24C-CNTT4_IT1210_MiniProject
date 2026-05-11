package com.example.smart_cinema_booking_system.service;

import com.example.smart_cinema_booking_system.dto.UserDTO;
import com.example.smart_cinema_booking_system.model.User;
import com.example.smart_cinema_booking_system.model.UserProfile;
import com.example.smart_cinema_booking_system.model.UserRole;
import com.example.smart_cinema_booking_system.repository.UserProfileRepository;
import com.example.smart_cinema_booking_system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    /**
     * Xử lý đăng ký người dùng mới và tạo Profile
     */
    @Transactional
    public String registerUser(UserDTO userDTO) {
        if (userRepository.findByUserName(userDTO.getUserName()).isPresent()) {
            return "Error: Username is already taken!";
        }

        // 1. Tạo và lưu User
        User user = User.builder()
                .userName(userDTO.getUserName())
                .password(BCrypt.hashpw(userDTO.getPassword(), BCrypt.gensalt()))
                .email(userDTO.getEmail())
                .fullName(userDTO.getFullName())
                .role(UserRole.CUSTOMER)
                .status(true)
                .build();

        User savedUser = userRepository.save(user);

        // 2. Tạo và lưu UserProfile tương ứng
        UserProfile profile = new UserProfile();
        profile.setUser(savedUser); // MapsId sẽ tự lấy ID của User
        profile.setFullName(userDTO.getFullName());
        profile.setEmail(userDTO.getEmail());
        userProfileRepository.save(profile);

        log.info("Registered new user and profile for: {}", userDTO.getUserName());
        return "User registered successfully!";
    }

    /**
     * Xử lý logic đăng nhập
     */
    public User login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUserName(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (BCrypt.checkpw(password, user.getPassword())) {
                log.info("Login successful for user: {} with role: {}", username, user.getRole());
                return user;
            } else {
                log.warn("Invalid password for user: {}", username);
            }
        } else {
            log.warn("User not found: {}", username);
        }
        return null;
    }
}
