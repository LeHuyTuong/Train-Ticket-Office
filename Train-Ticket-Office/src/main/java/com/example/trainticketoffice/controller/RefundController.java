package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Booking;
import com.example.trainticketoffice.model.RefundRequest;
import com.example.trainticketoffice.model.User;
import com.example.trainticketoffice.service.BookingService;
import com.example.trainticketoffice.service.RefundService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/refund") // Đường dẫn chính cho việc hoàn vé
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final BookingService bookingService;

    /**
     * Bước 1: Hiển thị form nhập thông tin ngân hàng.
     */
    @GetMapping("/request")
    public String showRefundForm(@RequestParam("bookingId") Long bookingId,
                                 Model model, HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        Optional<Booking> bookingOpt = bookingService.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy vé.");
            return "redirect:/bookings";
        }

        // Đóng gói 1 đối tượng RefundRequest rỗng để gửi ra form
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setBooking(bookingOpt.get());

        model.addAttribute("refundRequest", refundRequest);
        return "refund/form"; // Trả về file form.html (sẽ tạo ở bước 2)
    }

    /**
     * Bước 2: Xử lý submit form và tạo Yêu cầu Hoàn vé.
     */
    @PostMapping("/request")
    public String processRefundRequest(@ModelAttribute RefundRequest refundRequest,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("userLogin");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // Lấy bookingId từ object (do form gửi về)
            Long bookingId = refundRequest.getBooking().getBookingId();

            refundService.createRefundRequest(
                    bookingId,
                    currentUser,
                    refundRequest.getBankName(),
                    refundRequest.getBankAccountNumber(),
                    refundRequest.getAccountHolderName()
            );

            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu hoàn vé thành công! Vé của bạn đang ở trạng thái 'Chờ hoàn vé'.");
            return "redirect:/bookings"; // Quay về trang "Vé của tôi"

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/bookings";
        }
    }
}