package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.SeatType;
import com.example.trainticketoffice.service.SeatTypeService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/seat-types") // Đặt tên URL là /seat-types
public class SeatTypeController {

    private final SeatTypeService seatTypeService;

    @Autowired
    public SeatTypeController(SeatTypeService seatTypeService) {
        this.seatTypeService = seatTypeService;
    }

    @GetMapping
    public String listSeatTypes(Model model) {
        List<SeatType> seatTypes = seatTypeService.getAllSeatTypes();
        model.addAttribute("seatTypes", seatTypes);
        return "seattype/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("seatType", new SeatType());
        return "seattype/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<SeatType> seatType = seatTypeService.getSeatTypeById(id);
        if (seatType.isPresent()) {
            model.addAttribute("seatType", seatType.get());
            return "seattype/form";
        }
        return "redirect:/seat-types";
    }

    @PostMapping("/save")
    public String saveSeatType(@Valid @ModelAttribute("seatType") SeatType seatType,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "seattype/form";
        }

        try {
            seatTypeService.saveSeatType(seatType);
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu Loại Ghế thành công!");
            return "redirect:/seat-types";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi lưu: " + e.getMessage());
            return "redirect:/seat-types";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteSeatType(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            seatTypeService.deleteSeatType(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa Loại Ghế thành công.");
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa Loại Ghế này. Đã có Toa (Carriage) đang sử dụng nó.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/seat-types";
    }
}