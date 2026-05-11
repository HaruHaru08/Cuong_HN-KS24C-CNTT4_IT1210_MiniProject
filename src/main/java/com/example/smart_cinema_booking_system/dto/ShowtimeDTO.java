package com.example.smart_cinema_booking_system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ShowtimeDTO {

    private Integer id;

    @NotNull(message = "Phim không được để trống")
    private Integer movieId;

    private String movieTitle;
    private Integer movieDuration;

    @NotNull(message = "Phòng chiếu không được để trống")
    private Integer roomId;

    private String roomName;

    @NotNull(message = "Thời gian bắt đầu không được để trống")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal price;

    private Boolean status = true;

    private String errorMessage;

    private boolean hasConflict;

    private int ticketCount;

    private boolean hasPaidBookings;

    private LocalDateTime currentStartTime;
    private LocalDateTime currentEndTime;
    private Integer currentMovieId;
    private Integer currentRoomId;
    private BigDecimal currentPrice;
    private Boolean currentStatus;
    private String currentMovieTitle;
    private String currentRoomName;
}
