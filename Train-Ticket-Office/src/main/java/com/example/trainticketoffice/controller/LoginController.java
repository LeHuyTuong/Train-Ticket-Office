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
                return "redirect:/login";
            }
            session.setAttribute("userLogin", user);

            // ===== LOGIC CHUYỂN HƯỚNG NÂNG CAO =====
            String redirectUrl = (String) session.getAttribute("redirectAfterLogin");

            if (redirectUrl != null && !redirectUrl.isBlank()) {
                session.removeAttribute("redirectAfterLogin"); // Xóa session
                return "redirect:" + redirectUrl; // Chuyển đến nơi khách muốn
            }
            // ======================================


            if (user.getRole() == User.Role.STAFF) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/";
            }

        } catch (Exception e) {
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session){
        session.invalidate();
        return "redirect:/login";
    }
}
