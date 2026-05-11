package com.example.smart_cinema_booking_system.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "seat_type_config")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SeatTypeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "type_name", unique = true, nullable = false)
    private String typeName; // 'NORMAL', 'VIP', 'SWEETBOX'

    @Column(nullable = false)
    private BigDecimal surcharge; // Tiền cộng thêm
}
