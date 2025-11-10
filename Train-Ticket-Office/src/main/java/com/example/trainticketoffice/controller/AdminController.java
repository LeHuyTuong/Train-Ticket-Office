package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.service.AdminWalletService; // <-- THÊM IMPORT
import lombok.RequiredArgsConstructor; // <-- THÊM IMPORT
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // <-- THÊM IMPORT
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal; // <-- THÊM IMPORT

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminWalletService adminWalletService;

    // Xử lý trang chủ cho STAFF
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        // Lấy số dư và gửi ra view
        try {
            BigDecimal currentBalance = adminWalletService.getBalance();
            model.addAttribute("walletBalance", currentBalance);
        } catch (Exception e) {
            model.addAttribute("walletBalance", "Lỗi");
        }

        return "admin/dashboard";
    }
}