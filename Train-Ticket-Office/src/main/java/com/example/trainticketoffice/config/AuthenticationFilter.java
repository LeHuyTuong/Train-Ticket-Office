package com.example.trainticketoffice.config;

import com.example.trainticketoffice.model.User;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class AuthenticationFilter implements Filter {

    // Danh sách các đường dẫn CÔNG KHAI (không cần đăng nhập)
    private final List<String> publicPaths = List.of(
            "/login",
            "/images/",
            "/css/", // Thêm nếu bạn có thư mục css
            "/error"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestURI = request.getRequestURI();

        // 1. Kiểm tra xem đây có phải là đường dẫn công khai không
        // (Chúng ta cũng cho phép / (trang chủ) đi qua để họ tìm kiếm)
        boolean isPublicPath = publicPaths.stream().anyMatch(path -> requestURI.startsWith(path)) || requestURI.equals("/");

        if (isPublicPath && !requestURI.equals("/")) {
            // Lọc các đường dẫn tĩnh (css, images) và /login
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 2. Kiểm tra Session
        HttpSession session = request.getSession(false); // false = không tạo session mới

        if (session != null && session.getAttribute("userLogin") != null) {
            // Đã đăng nhập
            User user = (User) session.getAttribute("userLogin");

            // 3. Phân quyền
            if (requestURI.startsWith("/admin") && user.getRole() != User.Role.STAFF) {
                // Customer cố vào trang admin
                response.sendRedirect("/"); // Về trang chủ customer
                return;
            }
            if (requestURI.startsWith("/") && !requestURI.startsWith("/admin") && user.getRole() == User.Role.STAFF) {
                // Admin cố vào trang customer (trừ trang logout)
                if (!requestURI.startsWith("/logout")) {
                    response.sendRedirect("/admin/dashboard"); // Về trang chủ admin
                    return;
                }
            }

            filterChain.doFilter(servletRequest, servletResponse); // Cho qua
        } else {
            // Chưa đăng nhập

            // Nếu họ đang cố truy cập trang cần login (ví dụ /bookings)
            if (!isPublicPath) {
                response.sendRedirect("/login"); // Đá về trang login
            } else {
                filterChain.doFilter(servletRequest, servletResponse); // Cho qua (đối với trang /)
            }
        }
    }
}