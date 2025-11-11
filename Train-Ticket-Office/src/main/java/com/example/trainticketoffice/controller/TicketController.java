package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Ticket;
import com.example.trainticketoffice.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    //Hiển thị danh sách TẤT CẢ các vé (Trang Admin)
    @GetMapping
    public String listAllTickets(Model model) {
        List<Ticket> tickets = ticketService.findAll();
        model.addAttribute("tickets", tickets);
        return "admin/ticket/list";
    }

    //Xem chi tiết 1 vé (Trang Admin)
    @GetMapping("/{id}")
    public String viewTicketDetails(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        Optional<Ticket> ticketOpt = ticketService.findById(id);

        if (ticketOpt.isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Ticket not found with ID: " + id);
            return "redirect:/tickets";
        }

        model.addAttribute("ticket", ticketOpt.get());
        return "admin/ticket/detail";
    }

    //Xử lý Check-In (Hành động Admin)
    @GetMapping("/{id}/check-in")
    public String checkInTicket(@PathVariable("id") Long id, RedirectAttributes ra) {
        boolean success = ticketService.checkInTicket(id);
        if (success) {
            ra.addFlashAttribute("successMessage", "Ticket " + id + " checked in successfully!");
        } else {
            ra.addFlashAttribute("errorMessage", "Ticket could not be checked in (already checked in or not found).");
        }
        return "redirect:/tickets/" + id;
    }

    //Xử lý Hủy vé (Hành động Admin)
    @GetMapping("/{id}/cancel")
    public String cancelTicket(@PathVariable("id") Long id, RedirectAttributes ra) {
        boolean success = ticketService.cancelTicket(id);
        if (success) {
            ra.addFlashAttribute("successMessage", "Ticket " + id + " cancelled successfully!");
        } else {
            ra.addFlashAttribute("errorMessage", "Ticket could not be cancelled (already cancelled or checked in).");
        }
        return "redirect:/tickets/" + id;
    }
}