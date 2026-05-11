package com.example.smart_cinema_booking_system.config;

import com.example.smart_cinema_booking_system.model.Room;
import com.example.smart_cinema_booking_system.model.User;
import com.example.smart_cinema_booking_system.model.UserRole;
import com.example.smart_cinema_booking_system.repository.RoomRepository;
import com.example.smart_cinema_booking_system.repository.UserRepository;
import com.example.smart_cinema_booking_system.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final SeatService seatService;
    private final RoomRepository roomRepository;

    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            String commonPassword = "123456";
            String defaultAvatar = "https://via.placeholder.com/150/E50914/FFFFFF?text=SC";

            createOrUpdateUser(userRepository, "admin", commonPassword, "admin@cinema.com", "Vũ Văn Đoàn", UserRole.ADMIN, defaultAvatar);
            createOrUpdateUser(userRepository, "staff01", commonPassword, "staff@cinema.com", "Nguyễn Thành Nam", UserRole.STAFF, defaultAvatar);
            createOrUpdateUser(userRepository, "HeuQuang", commonPassword, "customer@gmail.com", "Hồ Quang Hêu", UserRole.CUSTOMER, defaultAvatar);

            log.info(">>> DataInitializer: Sample users updated");

            // Khởi tạo phòng chiếu
            initRooms();

            // Khởi tạo các loại ghế (normal, vip, sweetbox)
            seatService.initSeatTypes();

            // Tạo ghế cho tất cả các phòng
            seatService.initializeAllRoomsSeats();
        };
    }

    private void initRooms() {
        String[][] configs = {
            {"Phòng A - Standard", "Phòng A - Premium", "200"},
            {"Phòng B - Standard", "Phòng B - Standard", "250"},
            {"Phòng C - Mini",     "Phòng C - Budget",   "80"},
            {"Phòng D - IMAX",     "Phòng D - IMAX",     "450"},
            {"Phòng E - VIP",      "Phòng E - VIP",      "30"},
        };

        for (String[] cfg : configs) {
            String newName = cfg[0];
            String oldName = cfg[1];
            int seats = Integer.parseInt(cfg[2]);

            // Tìm phòng an toàn (không bị NullPointerException nếu name trong DB là null)
            Room room = roomRepository.findAll().stream()
                .filter(r -> newName.equals(r.getName()) || oldName.equals(r.getName()))
                .findFirst()
                .orElse(null);

            boolean isNew = room == null;
            if (isNew) {
                room = new Room();
            }

            room.setName(newName);
            room.setTotalSeats(seats);
            roomRepository.save(room);

            if (isNew) {
                log.info(">>> Tạo phòng '{}' ({} ghế)", newName, seats);
            }
        }
        log.info(">>> DataInitializer: Hoàn tất đồng bộ {} phòng chiếu", configs.length);
    }

    private void createOrUpdateUser(UserRepository repo, String username, String plainPassword, String email, String fullName, UserRole role, String avatarUrl) {
        Optional<User> existingUser = repo.findByUserName(username);
        User user;

        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setUserName(username);
        }

        user.setPassword(BCrypt.hashpw(plainPassword, BCrypt.gensalt()));
        user.setEmail(email);
        user.setFullName(fullName);
        user.setAvatarUrl(avatarUrl);
        user.setRole(role);
        user.setStatus(true);

        repo.save(user);
    }
}
