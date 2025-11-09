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

    // ===== SỬA LOGIC KIỂM TRA PATH =====

    // 1. Các đường dẫn phải khớp CHÍNH XÁC (Trang chủ, login, trang lỗi)
    private final List<String> publicExactPaths = List.of(
            "/",
            "/login",
            "/error"
    );

    // 2. Các đường dẫn chỉ cần khớp TIỀN TỐ (Thư mục tĩnh)
    private final List<String> publicPrefixPaths = List.of(
            "/images/",
            "/css/",
            "/js/" // Thêm nếu bạn có
    );

    // 3. Các đường dẫn của Admin (yêu cầu quyền STAFF)
    private final List<String> adminPaths = List.of(
            "/admin",
            "/routes",
            "/trips",   // (Trừ /trips/search và /trips/all)
            "/stations",
            "/trains",  // (Trừ /trains/all)
            "/carriages",
            "/seats",
            "/users",
            "/tickets"
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

    // Hàm kiểm tra Public path (ĐÃ SỬA)
    private boolean isPublicPath(String requestURI) {
        // Kiểm tra khớp chính xác
        if (publicExactPaths.contains(requestURI)) {
            return true;
        }
        // Kiểm tra khớp tiền tố
        for (String prefix : publicPrefixPaths) {
            if (requestURI.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // ======================================


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestURI = request.getRequestURI();

        // 1. Kiểm tra xem đây có phải là đường dẫn công khai không
        if (isPublicPath(requestURI)) {
            // Nếu là public (login, /, /images/...) -> Cho qua
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // 2. Kiểm tra Session (Từ đây, mọi trang đều yêu cầu login)
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userLogin") == null) {
            // Chưa đăng nhập -> Đá về trang login
            response.sendRedirect("/login");
            return;
        }

        // 3. Đã đăng nhập -> Lấy User và Phân quyền
        User user = (User) session.getAttribute("userLogin");
        boolean isRequestingAdminPath = isAdminPath(requestURI);

        if (user.getRole() == User.Role.STAFF) {
            // Người dùng là STAFF
            if (!isRequestingAdminPath && !requestURI.startsWith("/logout")) {
                // STAFF cố vào trang customer (ví dụ /bookings)
                response.sendRedirect("/admin/dashboard");
                return;
            }
        } else {
            // Người dùng là CUSTOMER
            if (isRequestingAdminPath) {

                response.sendRedirect("/"); // Về trang chủ customer
                return;
            }
        }

        // Cho phép truy cập (logout, hoặc đã đúng quyền)
        filterChain.doFilter(servletRequest, servletResponse);
    }
}