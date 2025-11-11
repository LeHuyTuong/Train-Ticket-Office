package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Station;
import com.example.trainticketoffice.service.StationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page; // <-- THÊM
import org.springframework.data.repository.query.Param; // <-- THÊM
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List; // <-- THÊM
import java.util.Optional;

@Controller
@RequestMapping("/stations")
public class StationController {

    private final StationService stationService;

    @Autowired
    public StationController(StationService stationService) {
        this.stationService = stationService;
    }


    @GetMapping
    public String listStations(Model model,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               @Param("keyword") String keyword) {

        Page<Station> stationPage = stationService.listAll(page, keyword);

        List<Station> stations = stationPage.getContent();
        long totalItems = stationPage.getTotalElements();
        int totalPages = stationPage.getTotalPages();

        model.addAttribute("stations", stations);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("keyword", keyword); // Gửi từ khóa ra view

        return "station/list";
    }


    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("station", new Station());
        model.addAttribute("statusTypes", new String[]{"ACTIVE", "INACTIVE"});
        return "station/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        Station station = stationService.getStationById(id);
        if (station != null) {
            model.addAttribute("station", station);
            model.addAttribute("statusTypes", new String[]{"ACTIVE", "INACTIVE"});
            return "station/form";
        }
        return "redirect:/stations";
    }

    @PostMapping("/save")
    public String saveStation(@Valid @ModelAttribute("station") Station station,
                              BindingResult result, Model model,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("statusTypes", new String[]{"ACTIVE", "INACTIVE"});
            return "station/form";
        }

        try {
            if (station.getId() == null) {
                Station createdStation = stationService.createStation(station);
                if (createdStation == null) {
                    model.addAttribute("errorMessage", "Station code already exists!");
                    model.addAttribute("statusTypes", new String[]{"ACTIVE", "INACTIVE"});
                    return "station/form";
                }
            } else {
                Station updatedStation = stationService.updateStation(station.getId(), station);
                if (updatedStation == null) {
                    model.addAttribute("errorMessage", "Station code already exists or station not found!");
                    model.addAttribute("statusTypes", new String[]{"ACTIVE", "INACTIVE"});
                    return "station/form";
                }
            }

            redirectAttributes.addFlashAttribute("successMessage", "Station saved successfully!");
            return "redirect:/stations";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error saving station: " + e.getMessage());
            model.addAttribute("statusTypes", new String[]{"ACTIVE", "INACTIVE"});
            return "station/form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteStation(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            stationService.deleteStation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Station with ID " + id + " has been deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting station: " + e.getMessage());
        }
        return "redirect:/stations";
    }
}