package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.common.TrainStatus; // <-- THÊM
import com.example.trainticketoffice.model.Train;
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
@RequestMapping("/trains")
public class TrainController {

    private final TrainService trainService;

    @Autowired
    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    private void addCommonAttributes(Model model) {
        model.addAttribute("allTrainStatus", TrainStatus.values());
    }

    @GetMapping
    public String listTrains(Model model) {
        List<Train> trains = trainService.getAllTrains();
        model.addAttribute("trains", trains);
        return "train/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("train", new Train());
        addCommonAttributes(model);
        return "train/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<Train> train = trainService.getTrainById(id);
        if (train.isPresent()) {
            model.addAttribute("train", train.get());
            addCommonAttributes(model);
            return "train/form";
        }
        return "redirect:/trains";
    }

    @PostMapping("/save")
    public String saveTrain(@Valid @ModelAttribute("train") Train train, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            addCommonAttributes(model);
            return "train/form";
        }

        try {
            trainService.saveTrain(train);
            redirectAttributes.addFlashAttribute("successMessage", "Train saved successfully!");
            return "redirect:/trains";
        } catch (IllegalStateException e) {
            model.addAttribute("errorMessage", e.getMessage());
            addCommonAttributes(model);
            return "train/form";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteTrain(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            trainService.deleteTrain(id);
            redirectAttributes.addFlashAttribute("successMessage", "Train with ID " + id + " has been deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting train: " + e.getMessage());
        }
        return "redirect:/trains";
    }
}