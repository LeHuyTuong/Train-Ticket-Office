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
@RequiredArgsConstructor // <-- THÊM ANNOTATION
public class AdminController {

    private final AdminWalletService adminWalletService; // <-- THÊM DÒNG NÀY

    // Xử lý trang chủ cho STAFF (đường dẫn "/admin/dashboard")
    @GetMapping("/dashboard")
    public String adminDashboard(Model model) { // <-- THÊM Model

        // Lấy số dư và gửi ra view
        try {
            BigDecimal currentBalance = adminWalletService.getBalance();
            model.addAttribute("walletBalance", currentBalance);
        } catch (Exception e) {
            model.addAttribute("walletBalance", "Lỗi");
        }

        return "admin/dashboard"; // Trả về file dashboard.html
    }
}