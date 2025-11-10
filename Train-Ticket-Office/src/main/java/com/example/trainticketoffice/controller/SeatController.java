package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.SeatStatus;
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
        Carriage selectedCarriage = null;

        if (carriageId != null) {
            Optional<Carriage> carriageOpt = allCarriages.stream()
                    .filter(carriage -> carriage.getCarriageId().equals(carriageId))
                    .findFirst();
            if (carriageOpt.isPresent()) {
                selectedCarriage = carriageOpt.get();
                seats = selectedCarriage.getSeats();
            } else {
                seats = List.of();
            }
        } else {
            seats = seatService.getAllSeats();
        }

        model.addAttribute("seats", seats);
        model.addAttribute("allCarriages", allCarriages);
        model.addAttribute("selectedCarriageId", carriageId);
        model.addAttribute("selectedCarriage", selectedCarriage);

        return "seat/list";
    }

    private void addCommonAttributes(Model model, Long carriageId) {
        if (carriageId != null) {
            model.addAttribute("allCarriages", carriageService.getCarriageById(carriageId).stream().toList());
            model.addAttribute("selectedCarriageId", carriageId);
        } else {
            model.addAttribute("allCarriages", carriageService.getAllCarriages());
        }
        model.addAttribute("seatStatusTypes", SeatStatus.values());
    }


    @GetMapping("/new")
    public String showCreateForm(Model model, @RequestParam("carriageId") Long carriageId) {
        Seat newSeat = new Seat();
        carriageService.getCarriageById(carriageId).ifPresent(newSeat::setCarriage);

        model.addAttribute("seat", newSeat);
        addCommonAttributes(model, carriageId);
        return "seat/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<Seat> seat = seatService.getSeatById(id);
        if (seat.isPresent()) {
            model.addAttribute("seat", seat.get());
            addCommonAttributes(model, seat.get().getCarriage().getCarriageId());
            return "seat/form";
        }
        return "redirect:/seats";
    }

    @PostMapping("/save")
    public String saveSeat(@Valid @ModelAttribute("seat") Seat seat, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        Long carriageId = null;
        if (seat.getCarriage() != null) {
            carriageId = seat.getCarriage().getCarriageId();
        }

        if (result.hasErrors()) {
            addCommonAttributes(model, carriageId);
            return "seat/form";
        }
        try {
            seatService.saveSeat(seat);
            redirectAttributes.addFlashAttribute("successMessage", "Ghế đã được lưu!");
            return "redirect:/seats?carriageId=" + seat.getCarriage().getCarriageId();
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addCommonAttributes(model, carriageId);
            return "seat/form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteSeat(@PathVariable("id") Long id, RedirectAttributes redirectAttributes,
                             @RequestParam(value = "carriageId", required = false) Long carriageId) {
        try {
            seatService.deleteSeat(id);
            redirectAttributes.addFlashAttribute("successMessage", "Ghế ID " + id + " đã được xóa.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa ghế: " + e.getMessage());
        }
        if (carriageId != null) {
            return "redirect:/seats?carriageId=" + carriageId;
        }
        return "redirect:/seats";
    }
}