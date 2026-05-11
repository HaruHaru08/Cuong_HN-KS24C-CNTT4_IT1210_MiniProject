package com.example.smart_cinema_booking_system.interceptor;

import com.example.smart_cinema_booking_system.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class ModelUserInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // Chỉ thực hiện khi có modelAndView (request trả về một view HTML)
        if (modelAndView != null && modelAndView.hasView()) {
            HttpSession session = request.getSession(false); // false để không tạo session mới nếu chưa có
            if (session != null) {
                User loggedInUser = (User) session.getAttribute("loggedInUser");
                if (loggedInUser != null) {
                    // Đưa thông tin user vào model để dùng trong Thymeleaf: ${loggedInUser}
                    modelAndView.addObject("loggedInUser", loggedInUser);
                }
            }
        }
    }
}
