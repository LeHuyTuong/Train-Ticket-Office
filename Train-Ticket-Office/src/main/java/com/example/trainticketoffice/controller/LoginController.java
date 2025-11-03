package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.User;
import com.example.trainticketoffice.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    @Autowired
    UserService userService;

    @GetMapping("/login")
    public String showLogin(){
        return "user/login"; // File login.html của bạn
    }

    @PostMapping("/login")
    public String processLogin(HttpSession session,
                               @RequestParam String email,
                               @RequestParam String password) {
        try {
            User user = userService.getUser(email, password);
            if (user == null) {
                // Nếu đăng nhập thất bại, quay lại trang login
                return "redirect:/login";
            }

            // Lưu thông tin user vào session
            session.setAttribute("userLogin", user);

            // ===== PHÂN LUỒNG VỀ 2 TRANG CHỦ MỚI =====
            if (user.getRole() == User.Role.STAFF) {
                // Nếu là STAFF, chuyển đến trang Dashboard của Admin
                return "redirect:/admin/dashboard";
            } else {
                // Nếu là CUSTOMER, chuyển đến trang chủ (homepage)
                return "redirect:/"; // Đường dẫn trang chủ
            }

        } catch (Exception e) {
            // Xử lý nếu có lỗi xảy ra
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/login";
    }
}