-- ============================================================
-- SHOWTIME SCHEDULING - SAMPLE DATA
-- Smart Cinema Booking System
-- ============================================================

-- Insert Movies (if not exists)
INSERT INTO movies (title, description, duration, release_date, poster_url, genre_id)
VALUES 
    ('Avatar: The Way of Water', 'A sequel to the blockbuster film', 193, '2022-12-16', '/uploads/avatar.jpg', 1),
    ('Inception', 'A skilled thief who steals corporate secrets', 148, '2010-07-16', '/uploads/inception.jpg', 2),
    ('The Dark Knight', 'When the menace known as the Joker emerges', 152, '2008-07-18', '/uploads/darkknight.jpg', 1),
    ('Interstellar', 'A team of explorers travel through a wormhole', 169, '2014-11-07', '/uploads/interstellar.jpg', 2),
    ('Dune', 'Paul Atreides travels to the dangerous planet', 166, '2021-10-01', '/uploads/dune.jpg', 1)
ON DUPLICATE KEY UPDATE title = title;

-- Insert Rooms (if not exists)
-- Sức chứa theo tiêu chuẩn rạp phim thực tế:
--   Standard: 150-300 ghế | Mini: 50-100 ghế | IMAX: 400-500+ ghế | VIP: 20-40 ghế
INSERT INTO rooms (name, total_seats)
VALUES 
    ('Phòng A - Standard', 200),
    ('Phòng B - Standard', 250),
    ('Phòng C - Mini', 80),
    ('Phòng D - IMAX', 450),
    ('Phòng E - VIP', 30)
ON DUPLICATE KEY UPDATE total_seats = total_seats;

-- Sample Showtimes for Today
INSERT INTO showtimes (movie_id, room_id, start_time, end_time, price, status)
VALUES 
    -- Today's schedule
    (1, 1, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 9 HOUR, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 11 HOUR + INTERVAL 30 MINUTE, 200000, 1),
    (2, 2, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 10 HOUR, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 12 HOUR + INTERVAL 15 MINUTE, 150000, 1),
    (3, 3, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 14 HOUR, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 16 HOUR + INTERVAL 15 MINUTE, 100000, 1),
    (4, 1, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 14 HOUR, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 16 HOUR + INTERVAL 30 MINUTE, 220000, 1),
    (5, 4, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 18 HOUR, DATE_ADD(NOW(), INTERVAL 0 DAY) + INTERVAL 20 HOUR + INTERVAL 15 MINUTE, 250000, 1),
    
    -- Tomorrow's schedule
    (1, 1, DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 10 HOUR, DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 12 HOUR + INTERVAL 30 MINUTE, 200000, 1),
    (2, 2, DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 14 HOUR, DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 16 HOUR + INTERVAL 15 MINUTE, 150000, 1),
    (3, 1, DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 17 HOUR, DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 19 HOUR + INTERVAL 15 MINUTE, 180000, 1),
    (4, 5, DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 19 HOUR, DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 21 HOUR + INTERVAL 30 MINUTE, 250000, 1);

-- Sample Showtimes for Next Week
INSERT INTO showtimes (movie_id, room_id, start_time, end_time, price, status)
VALUES 
    (5, 2, DATE_ADD(NOW(), INTERVAL 7 DAY) + INTERVAL 10 HOUR, DATE_ADD(NOW(), INTERVAL 7 DAY) + INTERVAL 12 HOUR + INTERVAL 15 MINUTE, 200000, 1),
    (1, 3, DATE_ADD(NOW(), INTERVAL 7 DAY) + INTERVAL 14 HOUR, DATE_ADD(NOW(), INTERVAL 7 DAY) + INTERVAL 16 HOUR + INTERVAL 30 MINUTE, 150000, 1),
    (2, 4, DATE_ADD(NOW(), INTERVAL 7 DAY) + INTERVAL 18 HOUR, DATE_ADD(NOW(), INTERVAL 7 DAY) + INTERVAL 20 HOUR + INTERVAL 15 MINUTE, 220000, 1),
    (3, 5, DATE_ADD(NOW(), INTERVAL 8 DAY) + INTERVAL 10 HOUR, DATE_ADD(NOW(), INTERVAL 8 DAY) + INTERVAL 12 HOUR + INTERVAL 15 MINUTE, 200000, 1),
    (4, 1, DATE_ADD(NOW(), INTERVAL 8 DAY) + INTERVAL 15 HOUR, DATE_ADD(NOW(), INTERVAL 8 DAY) + INTERVAL 17 HOUR + INTERVAL 30 MINUTE, 230000, 1);

-- ============================================================
-- IMPORTANT NOTES:
-- ============================================================
-- 1. Buffer time 15 minutes is automatically added by the service
-- 2. Adjust movie durations as needed:
--    - Avatar: 193 minutes
--    - Inception: 148 minutes
--    - The Dark Knight: 152 minutes
--    - Interstellar: 169 minutes
--    - Dune: 166 minutes
-- 3. Price format: Vietnamese Dong (VNĐ)
-- 4. Status: 1 = Active, 0 = Inactive
-- 5. Use the INSERT IGNORE or ON DUPLICATE KEY UPDATE to prevent duplicates
