package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.RefundRequest;
import com.example.trainticketoffice.model.User;
import com.example.trainticketoffice.service.RefundService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/refunds") // URL riêng cho Admin
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    /**
     * Hiển thị danh sách các vé đang chờ hoàn (PENDING)
     */
    @GetMapping
    public String listPendingRefunds(Model model) {
        List<RefundRequest> pendingRefunds = refundService.getPendingRefunds();
        model.addAttribute("refundRequests", pendingRefunds);
        return "refund/admin-list"; // Trả về file HTML (sẽ tạo ở bước 3)
    }

    /**
     * Xử lý khi Admin bấm "Duyệt Hoàn Vé"
     */
    @PostMapping("/approve/{id}")
    public String approveRefund(@PathVariable("id") Long refundRequestId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        User adminUser = (User) session.getAttribute("userLogin");
        if (adminUser == null || adminUser.getRole() != User.Role.STAFF) {
            return "redirect:/login"; // Cần quyền Admin
        }

        try {
            refundService.approveRefund(refundRequestId, adminUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt hoàn vé thành công. Ghế đã được giải phóng.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }

    /**
     * Xử lý khi Admin bấm "Từ chối"
     */
    @PostMapping("/reject/{id}")
    public String rejectRefund(@PathVariable("id") Long refundRequestId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        User adminUser = (User) session.getAttribute("userLogin");
        if (adminUser == null || adminUser.getRole() != User.Role.STAFF) {
            return "redirect:/login";
        }

        try {
            refundService.rejectRefund(refundRequestId, adminUser);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối hoàn vé. Vé đã được trả về trạng thái 'Đã thanh toán'.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/refunds";
    }
}