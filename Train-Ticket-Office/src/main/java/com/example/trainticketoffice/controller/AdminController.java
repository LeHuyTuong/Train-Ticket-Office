package com.example.trainticketoffice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // Xử lý trang chủ cho STAFF (đường dẫn "/admin/dashboard")
    @GetMapping("/dashboard")
    public String adminDashboard() {
        return "admin/dashboard"; // Trả về file dashboard.html
    }
}