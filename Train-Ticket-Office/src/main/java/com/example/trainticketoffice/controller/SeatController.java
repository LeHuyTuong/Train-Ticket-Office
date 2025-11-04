package com.example.trainticketoffice.controller;
import com.example.trainticketoffice.model.Carriage;
import com.example.trainticketoffice.model.Seat;
import com.example.trainticketoffice.service.CarriageService;
import com.example.trainticketoffice.service.SeatService;
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
    private final CarriageService carriageService;
    @Autowired
    public SeatController(SeatService seatService, CarriageService carriageService) {
        this.seatService = seatService;
        this.carriageService = carriageService;
    }
    @GetMapping
    public String listSeats(Model model, @RequestParam(value = "carriageId", required = false) Long carriageId) {
        List<Seat> seats;
        List<Carriage> allCarriages = carriageService.getAllCarriages();
        if (carriageId != null) {
            seats = allCarriages.stream()
                    .filter(carriage -> carriage.getCarriageId().equals(carriageId))
                    .findFirst()
                    .map(Carriage::getSeats)
                    .orElse(List.of());
        } else {
            seats = seatService.getAllSeats();
        }
        model.addAttribute("seats", seats);
        model.addAttribute("allCarriages", allCarriages);
        model.addAttribute("selectedCarriageId", carriageId);
        return "seat/list";
    }
    private void addCommonAttributes(Model model) {
        List<Carriage> allCarriages = carriageService.getAllCarriages();
        model.addAttribute("allCarriages", allCarriages);
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
            return "redirect:/seats?carriageId=" + seat.getCarriage().getCarriageId();
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