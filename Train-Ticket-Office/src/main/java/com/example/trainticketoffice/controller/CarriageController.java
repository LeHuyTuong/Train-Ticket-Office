package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Carriage;
import com.example.trainticketoffice.model.SeatType;
import com.example.trainticketoffice.model.Train;
import com.example.trainticketoffice.repository.SeatTypeRepository;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/carriages")
public class CarriageController {

    private final CarriageService carriageService;
    private final TrainService trainService;
    private final SeatTypeRepository seatTypeRepository;

    @Autowired
    public CarriageController(CarriageService carriageService, TrainService trainService, SeatTypeRepository seatTypeRepository) {
        this.carriageService = carriageService;
        this.trainService = trainService;
        this.seatTypeRepository = seatTypeRepository;
    }

    @GetMapping
    public String listCarriages(Model model,
                                @RequestParam(value = "trainId", required = false) Long trainId) {

        List<Carriage> carriages;
        List<Train> allTrains = trainService.getAllTrains();
        Train selectedTrain = null;

        if (trainId != null) {
            Optional<Train> trainOpt = allTrains.stream()
                    .filter(train -> train.getId().equals(trainId))
                    .findFirst();
            if (trainOpt.isPresent()) {
                selectedTrain = trainOpt.get();
                carriages = selectedTrain.getCarriages();
            } else {
                carriages = List.of();
            }
        } else {
            carriages = carriageService.getAllCarriages();
        }

        model.addAttribute("carriages", carriages);
        model.addAttribute("allTrains", allTrains);
        model.addAttribute("selectedTrainId", trainId);
        model.addAttribute("selectedTrain", selectedTrain);
        return "carriage/list";
    }


    private void addCommonAttributes(Model model, Long trainId) {
        if(trainId != null) {
            model.addAttribute("allTrains", trainService.getTrainById(trainId).stream().toList());
            model.addAttribute("selectedTrainId", trainId);
        } else {
            model.addAttribute("allTrains", trainService.getAllTrains());
        }

        List<SeatType> allSeatTypes = seatTypeRepository.findAll();
        model.addAttribute("allSeatTypes", allSeatTypes);
    }


    @GetMapping("/new")
    public String showCreateForm(Model model, @RequestParam(value = "trainId") Long trainId) {
        Carriage carriage = new Carriage();
        trainService.getTrainById(trainId).ifPresent(carriage::setTrain);
        model.addAttribute("carriage", carriage);
        addCommonAttributes(model, trainId);
        return "carriage/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        Optional<Carriage> carriage = carriageService.getCarriageById(id);
        if (carriage.isPresent()) {
            model.addAttribute("carriage", carriage.get());
            addCommonAttributes(model, carriage.get().getTrain().getId());
            return "carriage/form";
        }
        return "redirect:/carriages";
    }

    @PostMapping("/save")
    public String saveCarriage(@Valid @ModelAttribute("carriage") Carriage carriage,
                               BindingResult result, Model model,
                               RedirectAttributes redirectAttributes) {

        Long trainId = (carriage.getTrain() != null) ? carriage.getTrain().getId() : null;

        if (result.hasErrors()) {
            addCommonAttributes(model, trainId);
            return "carriage/form";
        }

        try {
            carriageService.saveCarriage(carriage);
            redirectAttributes.addFlashAttribute("successMessage", "Carriage saved successfully!");
            return "redirect:/carriages?trainId=" + trainId;
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Error saving carriage: " + e.getMessage());
            addCommonAttributes(model, trainId);
            return "carriage/form";
        }
    }


    @GetMapping("/delete/{id}")
    public String deleteCarriage(@PathVariable("id") Long id,
                                 @RequestParam(value = "trainId", required = false) Long trainId,
                                 RedirectAttributes redirectAttributes) {
        try {
            carriageService.deleteCarriage(id);
            redirectAttributes.addFlashAttribute("successMessage", "Carriage with ID " + id + " has been deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting carriage: " + e.getMessage());
        }

        if (trainId != null) {
            return "redirect:/carriages?trainId=" + trainId;
        }
        return "redirect:/carriages";
    }
}