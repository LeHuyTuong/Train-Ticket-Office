package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.model.Train;
import com.example.trainticketoffice.service.SeatService;
import com.example.trainticketoffice.service.TrainService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/seats")
public class SeatController {

    private final SeatService seatService;
    private final TrainService trainService;

    @Autowired
    public SeatController(SeatService seatService, TrainService trainService) {
        this.seatService = seatService;
        this.trainService = trainService;
    }

    @GetMapping
    public String listSeats(Model model) {
        List<Seat> seats = seatService.getAllSeats();
        model.addAttribute("seats", seats);
        return "seat/list";
    }

    private void addCommonAttributes(Model model) {
        List<Train> allTrains = trainService.getAllTrains();
        model.addAttribute("allTrains", allTrains);
        model.addAttribute("seatTypes", new String[]{"normal", "vip"});
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("seat", new Seat());
        addCommonAttributes(model);
        return "seat/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<Seat> seat = seatService.getSeatById(id);
        if (seat.isPresent()) {
            model.addAttribute("seat", seat.get());
            addCommonAttributes(model);
            return "seat/form";
        }
        return "redirect:/seats";
    }

    @PostMapping("/save")
    public String saveSeat(@Valid @ModelAttribute("seat") Seat seat, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addCommonAttributes(model);
            return "seat/form";
        }

        try {
            seatService.saveSeat(seat);
            redirectAttributes.addFlashAttribute("successMessage", "Seat saved successfully!");
            return "redirect:/seats";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addCommonAttributes(model);
            return "seat/form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteSeat(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            seatService.deleteSeat(id);
            redirectAttributes.addFlashAttribute("successMessage", "Seat with ID " + id + " has been deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting seat: " + e.getMessage());
        }
        return "redirect:/seats";
    }
}
