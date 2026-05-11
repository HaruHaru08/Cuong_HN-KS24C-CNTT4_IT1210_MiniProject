package com.example.smart_cinema_booking_system.service;

import com.example.smart_cinema_booking_system.model.Room;
import com.example.smart_cinema_booking_system.model.Seat;
import com.example.smart_cinema_booking_system.model.SeatTypeConfig;
import com.example.smart_cinema_booking_system.repository.RoomRepository;
import com.example.smart_cinema_booking_system.repository.SeatRepository;
import com.example.smart_cinema_booking_system.repository.SeatTypeConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;
    private final SeatTypeConfigRepository seatTypeConfigRepository;

    /**
     * Khởi tạo các loại ghế mặc định: normal, vip, sweetbox
     */
    @Transactional
    public void initSeatTypes() {
        createSeatTypeIfNotExist("normal", BigDecimal.valueOf(0));
        createSeatTypeIfNotExist("vip", BigDecimal.valueOf(30000));
        createSeatTypeIfNotExist("sweetbox", BigDecimal.valueOf(60000));
        log.info(">>> Đã khởi tạo các loại ghế: normal (0đ), vip (+30,000đ), sweetbox (+60,000đ)");
    }

    private void createSeatTypeIfNotExist(String typeName, BigDecimal surcharge) {
        try {
            if (seatTypeConfigRepository.findByTypeName(typeName).isEmpty()) {
                SeatTypeConfig config = new SeatTypeConfig();
                config.setTypeName(typeName);
                config.setSurcharge(surcharge);
                seatTypeConfigRepository.save(config);
                log.info(">>> Đã tạo loại ghế: {} (phụ thu {})", typeName, surcharge);
            } else {
                log.info(">>> Loại ghế '{}' đã tồn tại", typeName);
            }
        } catch (Exception e) {
            log.error(">>> LỖI khi tạo loại ghế '{}': {}", typeName, e.getMessage(), e);
        }
    }

    /**
     * Tạo ghế cho tất cả các phòng (nếu chưa có ghế)
     */
    @Transactional
    public void initializeAllRoomsSeats() {
        List<Room> rooms = roomRepository.findAll();
        log.info(">>> Bắt đầu tạo ghế cho {} phòng...", rooms.size());

        if (rooms.isEmpty()) {
            log.warn(">>> Không có phòng nào trong database! Hãy thêm phòng trước.");
            return;
        }

        for (Room room : rooms) {
            try {
                long existingCount = seatRepository.countByRoomId(room.getId());
                if (existingCount == 0) {
                    initializeRoomSeats(room);
                } else {
                    log.info(">>> Phòng {} (ID={}) đã có {} ghế, bỏ qua", room.getName(), room.getId(), existingCount);
                }
            } catch (Exception e) {
                log.error(">>> LỖI khi tạo ghế cho phòng {} (ID={}): {}", room.getName(), room.getId(), e.getMessage(), e);
            }
        }
        log.info(">>> Hoàn tất tạo ghế cho tất cả phòng!");
    }

    /**
     * Tạo lại ghế cho tất cả phòng (xóa cũ, tạo mới)
     * Gọi từ admin endpoint "Tạo ghế" để chạy thủ công
     */
    @Transactional
    public void regenerateAllRoomSeats() {
        List<Room> rooms = roomRepository.findAll();
        log.info(">>> Bắt đầu TẠO LẠI ghế cho {} phòng...", rooms.size());

        for (Room room : rooms) {
            try {
                initializeRoomSeats(room);
            } catch (Exception e) {
                log.error(">>> LỖI khi tạo lại ghế cho phòng {} (ID={}): {}", room.getName(), room.getId(), e.getMessage(), e);
            }
        }
        log.info(">>> Hoàn tất tạo lại ghế cho tất cả phòng!");
    }

    /**
     * Tạo lại ghế cho một phòng (xóa cũ, tạo mới)
     * Gọi từ admin endpoint để chạy thủ công
     */
    @Transactional
    public boolean regenerateRoomSeats(Integer roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null) {
            log.error(">>> Không tìm thấy phòng ID={}", roomId);
            return false;
        }
        initializeRoomSeats(room);
        return true;
    }

    /**
     * Tạo ghế cho một phòng với sơ đồ giống rạp chiếu phim thực tế.
     *
     * Cách bố trí theo từng loại phòng:
     *
     *   IMAX/Large (450 ghế)    : 23 hàng (A-W), 20 ghế/hàng
     *   Standard (200-250 ghế)  : 15-18 hàng (A-O đến A-R), 14-16 ghế/hàng
     *   Mini (80 ghế)           : 8 hàng (A-H), 10 ghế/hàng
     *   VIP (30 ghế)            : 5 hàng (A-E), 6 ghế/hàng
     *
     * - Hàng ghế: A, B, C, D, E... (từ màn hình ra phía sau)
     * - Loại ghế:
     *   + Normal: 2-3 hàng đầu (gần màn hình, giá cơ bản)
     *   + VIP: các hàng giữa (tầm nhìn đẹp nhất, phụ thu 30k)
     *   + Sweetbox: 1-2 hàng cuối (ghế đôi, phụ thu 60k)
     */
    @Transactional
    public void initializeRoomSeats(Room room) {
        log.info(">>> Bắt đầu tạo ghế cho phòng {} (ID={}, {} ghế)...", room.getName(), room.getId(), room.getTotalSeats());

        // Xóa ghế cũ nếu có
        seatRepository.deleteByRoomId(room.getId());
        log.info(">>> Đã xóa ghế cũ của phòng {}", room.getName());

        int totalSeats = room.getTotalSeats();

        // Xác định số ghế mỗi hàng dựa trên sức chứa phòng
        int seatsPerRow = determineSeatsPerRow(totalSeats);

        // Tính số hàng ghế
        int numRows = (int) Math.ceil((double) totalSeats / seatsPerRow);
        int lastRowSeats = totalSeats - (numRows - 1) * seatsPerRow;
        if (lastRowSeats <= 0) {
            lastRowSeats = seatsPerRow;
        }

        // Xác định số hàng cho mỗi loại ghế
        int sweetboxRows = Math.max(1, (int) Math.round(numRows * 0.2));   // 20% cuối
        int normalRows = Math.max(2, (int) Math.round(numRows * 0.25));    // 25% đầu
        int vipEndRow = numRows - sweetboxRows;

        log.info(">>> Bố trí: {} hàng (A-{}), {} ghế/hàng, Normal={} hàng, VIP={} hàng, Sweetbox={} hàng",
                numRows, (char) ('A' + numRows - 1), seatsPerRow, normalRows,
                vipEndRow - normalRows, sweetboxRows);

        List<Seat> batch = new ArrayList<>();
        int seatCount = 0;

        for (int row = 0; row < numRows; row++) {
            char rowLetter = (char) ('A' + row);
            int seatsInThisRow = (row == numRows - 1) ? lastRowSeats : seatsPerRow;

            // Xác định loại ghế cho hàng này
            String typeName;
            if (row < normalRows) {
                typeName = "normal";
            } else if (row < vipEndRow) {
                typeName = "vip";
            } else {
                typeName = "sweetbox";
            }

            SeatTypeConfig seatType = seatTypeConfigRepository.findByTypeName(typeName).orElse(null);
            if (seatType == null) {
                log.warn(">>> Loại ghế '{}' chưa tồn tại, tạo mới...", typeName);
                seatType = createAndSaveSeatType(typeName, typeName.equals("vip") ? BigDecimal.valueOf(30000) :
                        typeName.equals("sweetbox") ? BigDecimal.valueOf(60000) : BigDecimal.ZERO);
            }

            for (int col = 1; col <= seatsInThisRow; col++) {
                Seat seat = new Seat();
                seat.setRoom(room);
                seat.setSeatNumber("" + rowLetter + col);
                seat.setSeatType(seatType);
                batch.add(seat);
                seatCount++;
            }
        }

        seatRepository.saveAll(batch);
        log.info(">>> ĐÃ TẠO THÀNH CÔNG {} ghế cho phòng {} ({} ghế/hàng, {} hàng: A-{})",
                seatCount, room.getName(), seatsPerRow, numRows, (char) ('A' + numRows - 1));
    }

    private SeatTypeConfig createAndSaveSeatType(String typeName, BigDecimal surcharge) {
        SeatTypeConfig config = new SeatTypeConfig();
        config.setTypeName(typeName);
        config.setSurcharge(surcharge);
        return seatTypeConfigRepository.save(config);
    }

    /**
     * Xác định số ghế mỗi hàng dựa trên tổng sức chứa phòng
     * Giống rạp thực tế:
     *   - IMAX/Large (400-500+ ghế): 20-22 ghế/hàng (rạp IMAX thực tế có 20-26 ghế/hàng)
     *   - Standard (150-300 ghế): 12-14 ghế/hàng
     *   - Mini (50-100 ghế): 8-10 ghế/hàng
     *   - VIP (20-40 ghế): 6-8 ghế/hàng (ghế sofa rộng)
     */
    private int determineSeatsPerRow(int totalSeats) {
        if (totalSeats >= 400) return 20;
        if (totalSeats >= 250) return 16;
        if (totalSeats >= 150) return 14;
        if (totalSeats >= 100) return 12;
        if (totalSeats >= 60) return 10;
        if (totalSeats >= 40) return 8;
        return 6;
    }

    /**
     * Lấy danh sách ghế theo phòng
     */
    public List<Seat> getSeatsByRoom(Integer roomId) {
        return seatRepository.findByRoomId(roomId);
    }

    /**
     * Lấy sơ đồ ghế theo phòng (nhóm theo hàng)
     * Trả về Map<String, List<Seat>> với key là tên hàng (A, B, C...)
     */
    public java.util.Map<String, List<Seat>> getSeatMapByRoom(Integer roomId) {
        List<Seat> seats = seatRepository.findByRoomId(roomId);
        java.util.Map<String, List<Seat>> seatMap = new java.util.TreeMap<>();

        for (Seat seat : seats) {
            String row = seat.getSeatNumber().replaceAll("[0-9]", "");
            seatMap.computeIfAbsent(row, k -> new ArrayList<>()).add(seat);
        }

        return seatMap;
    }
}
