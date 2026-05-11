package com.example.smart_cinema_booking_system.interceptor;

import com.example.smart_cinema_booking_system.model.User;
import com.example.smart_cinema_booking_system.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        HttpSession session = request.getSession();
        User loggedInUser = (User) session.getAttribute("loggedInUser");
        String requestURI = request.getRequestURI();

        // Chỉ kiểm tra các đường dẫn nhạy cảm
        boolean isProtectedPath = requestURI.startsWith("/admin") || 
                                 requestURI.startsWith("/staff") || 
                                 requestURI.startsWith("/customer");

        if (isProtectedPath) {
            // Nếu vào trang bảo vệ mà chưa đăng nhập
            if (loggedInUser == null) {
                response.sendRedirect("/auth/login?error=unauthorized");
                return false;
            }

            UserRole role = loggedInUser.getRole();

            // Kiểm tra quyền Admin
            if (requestURI.startsWith("/admin") && role != UserRole.ADMIN) {
                response.sendRedirect("/auth/login?error=access-denied");
                return false;
            }

            // Kiểm tra quyền Staff
            if (requestURI.startsWith("/staff") && (role != UserRole.STAFF && role != UserRole.ADMIN)) {
                response.sendRedirect("/auth/login?error=access-denied");
                return false;
            }
        }

        return true;
    }
}
