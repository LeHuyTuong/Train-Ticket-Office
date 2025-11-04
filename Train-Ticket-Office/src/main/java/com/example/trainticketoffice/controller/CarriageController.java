package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Carriage;
import com.example.trainticketoffice.model.Train;
import com.example.trainticketoffice.service.CarriageService;
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
import java.util.stream.Collectors; // <-- THÊM IMPORT

@Controller
@RequestMapping("/carriages")
public class CarriageController {

    private final CarriageService carriageService;
    private final TrainService trainService;

    @Autowired
    public CarriageController(CarriageService carriageService, TrainService trainService) {
        this.carriageService = carriageService;
        this.trainService = trainService;
    }

    // ===== SỬA HÀM NÀY =====
    @GetMapping
    public String listCarriages(Model model,
                                @RequestParam(value = "trainId", required = false) Long trainId) {

        List<Carriage> carriages;
        List<Train> allTrains = trainService.getAllTrains(); // Lấy tất cả tàu

        if (trainId != null) {
            // Nếu có trainId, lọc các toa theo tàu đó
            carriages = allTrains.stream()
                    .filter(train -> train.getId().equals(trainId))
                    .findFirst()
                    .map(Train::getCarriages) // Lấy danh sách toa từ tàu
                    .orElse(List.of()); // Nếu không tìm thấy, trả về ds rỗng
        } else {
            // Nếu không có trainId, hiển thị tất cả
            carriages = carriageService.getAllCarriages();
        }

        model.addAttribute("carriages", carriages);
        model.addAttribute("allTrains", allTrains); // Gửi danh sách tàu ra view
        model.addAttribute("selectedTrainId", trainId); // Gửi ID đã chọn để giữ bộ lọc
        return "carriage/list";
    }

    // Dùng chung để gửi danh sách Tàu (Trains) sang form
    private void addCommonAttributes(Model model) {
        List<Train> allTrains = trainService.getAllTrains();
        model.addAttribute("allTrains", allTrains);
    }

    // Hiển thị form Thêm Mới
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("carriage", new Carriage());
        addCommonAttributes(model);
        return "carriage/form";
    }

    // Hiển thị form Chỉnh Sửa
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<Carriage> carriage = carriageService.getCarriageById(id);
        if (carriage.isPresent()) {
            model.addAttribute("carriage", carriage.get());
            addCommonAttributes(model);
            return "carriage/form";
        }
        return "redirect:/carriages";
    }

    // Xử lý Lưu (Thêm mới hoặc Cập nhật)
    @PostMapping("/save")
    public String saveCarriage(@Valid @ModelAttribute("carriage") Carriage carriage,
                               BindingResult result, Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addCommonAttributes(model);
            return "carriage/form";
        }

        try {
            carriageService.saveCarriage(carriage);
            redirectAttributes.addFlashAttribute("successMessage", "Carriage saved successfully!");
            // Sửa: Quay về trang lọc của tàu đó
            return "redirect:/carriages?trainId=" + carriage.getTrain().getId();
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error saving carriage: " + e.getMessage());
            addCommonAttributes(model);
            return "carriage/form";
        }
    }

    // Xử lý Xóa
    @GetMapping("/delete/{id}")
    public String deleteCarriage(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            carriageService.deleteCarriage(id);
            redirectAttributes.addFlashAttribute("successMessage", "Carriage with ID " + id + " has been deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting carriage: " + e.getMessage());
        }
        return "redirect:/carriages";
    }
}