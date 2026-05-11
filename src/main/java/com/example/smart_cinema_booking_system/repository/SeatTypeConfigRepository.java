package com.example.smart_cinema_booking_system.repository;

import com.example.smart_cinema_booking_system.model.SeatTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeatTypeConfigRepository extends JpaRepository<SeatTypeConfig, Integer> {
    
    /**
     * Tìm loại ghế theo tên
     */
    Optional<SeatTypeConfig> findByTypeName(String typeName);
}
