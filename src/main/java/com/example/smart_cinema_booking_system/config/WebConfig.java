package com.example.smart_cinema_booking_system.config;

import com.example.smart_cinema_booking_system.interceptor.ModelUserInterceptor;
import com.example.smart_cinema_booking_system.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Autowired
    private ModelUserInterceptor modelUserInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor thêm user vào Model - đảm bảo Thymeleaf luôn có access đến user info
        // Cần chạy TRƯỚC RoleInterceptor
        registry.addInterceptor(modelUserInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/error"
                );

        // Interceptor kiểm tra role - chặn access không được phép
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",                // Không chặn trang chủ để HomeController xử lý điều hướng
                        "/auth/**",         // Không chặn trang login/register
                        "/css/**", 
                        "/js/**", 
                        "/images/**", 
                        "/error",
                        "/access-denied"
                );
    }
}
