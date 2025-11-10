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

    // 1. Các đường dẫn phải khớp CHÍNH XÁC
    private final List<String> publicExactPaths = List.of(
            "/",
            "/login",
            "/error"
    );

    // 2. Các đường dẫn chỉ cần khớp TIỀN TỐ
    private final List<String> publicPrefixPaths = List.of(
            "/images/",
            "/css/",
            "/js/"
    );

    private final List<String> adminPaths = List.of(
            "/admin",
            "/routes",
            "/trips",
            "/stations",
            "/trains",
            "/carriages",
            "/users",
            "/tickets",
            "/seat-types",
            "/seats",
            "/admin/refunds"
    );

    // Hàm kiểm tra Admin path (đã xử lý ngoại lệ)
    private boolean isAdminPath(String requestURI) {
        if (requestURI.equals("/trips/search") || requestURI.equals("/trips/all")) {
            return false;
        }
        if (requestURI.equals("/trains/all")) {
            return false;
        }
        return adminPaths.stream().anyMatch(path -> requestURI.startsWith(path));
    }

    // Hàm kiểm tra Public path
    private boolean isPublicPath(String requestURI) {
        if (publicExactPaths.contains(requestURI)) {
            return true;
        }
        for (String prefix : publicPrefixPaths) {
            if (requestURI.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestURI = request.getRequestURI();

        // 1. Cho phép tất cả các trang Public (/, /login, /images...)
        if (isPublicPath(requestURI)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 2. Kiểm tra Session (Từ đây, mọi trang đều yêu cầu login)
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userLogin") == null) {
            response.sendRedirect("/login");
            return;
        }

        // 3. ĐÃ ĐĂNG NHẬP

        // ===== SỬA LỖI LOGOUT Ở ĐÂY =====
        // Cho phép /logout đi qua để đến LoginController
        if (requestURI.startsWith("/logout")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }
        // ================================

        User user = (User) session.getAttribute("userLogin");
        boolean isRequestingAdminPath = isAdminPath(requestURI);

        if (user.getRole() == User.Role.STAFF) {
            // (STAFF)
            if (!isRequestingAdminPath) {
                // Nếu Staff cố vào trang customer (như /bookings) -> Về dashboard admin
                response.sendRedirect("/admin/dashboard");
                return;
            }
        } else {
            // (CUSTOMER)
            if (isRequestingAdminPath) {
                // Nếu Customer cố vào trang admin (/admin, /routes...) -> Về trang chủ customer
                response.sendRedirect("/");
                return;
            }
        }

        // (Nếu đúng quyền: Staff vào admin, Customer vào /bookings...)
        filterChain.doFilter(servletRequest, servletResponse);
    }
}