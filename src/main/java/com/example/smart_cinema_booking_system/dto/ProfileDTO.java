package com.example.smart_cinema_booking_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ProfileDTO {
    private Integer userId;

    @NotBlank(message = "Họ và tên không được để trống")
    private String fullName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Số điện thoại không hợp lệ (10 chữ số)")
    private String phone;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;
}
