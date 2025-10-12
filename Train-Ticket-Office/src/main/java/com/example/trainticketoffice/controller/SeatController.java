package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.service.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/seats")
public class SeatController {

    private final SeatService seatService;

    @Autowired
    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    // Hien thi danh sach tat ca seat
    @GetMapping
    public String listSeats(Model model) {
        List<Seat> seatList = seatService.getAllSeats();
        model.addAttribute("seats", seatList);
        return "seat/list"; // templates/seat/list.html
    }

    // Hien thi form tao moi
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("seat", new Seat());
        return "seat/form"; // templates/seat/form.html
    }

    // Hien thi form sua
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<Seat> seatOptional = seatService.getSeatById(id);
        if (seatOptional.isPresent()) {
            model.addAttribute("seat", seatOptional.get());
            return "seat/form";
        }
        return "redirect:/seats";
    }

    // Luu (them moi hoac cap nhat)
    @PostMapping("/save")
    public String saveSeat(@ModelAttribute("seat") Seat seat) {
        if (seat.getSeatId() != null && seat.getSeatId() != 0) {
            seatService.updateSeat(seat.getSeatId(), seat);
        } else {
            seatService.createSeat(seat);
        }
        return "redirect:/seats";
    }

    // Xoa seat
    @GetMapping("/delete/{id}")
    public String deleteSeat(@PathVariable("id") Long id) {
        try {
            seatService.deleteSeat(id);
        } catch (RuntimeException e) {
            System.err.println("Loi khi xoa seat ID " + id + ": " + e.getMessage());
        }
        return "redirect:/seats";
    }
}
