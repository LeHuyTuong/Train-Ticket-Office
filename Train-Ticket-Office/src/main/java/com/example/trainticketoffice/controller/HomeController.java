package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Train; // <-- THÊM
import com.example.trainticketoffice.service.StationService;
import com.example.trainticketoffice.service.TrainService; // <-- THÊM
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List; // <-- THÊM

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final StationService stationService;
    private final TrainService trainService; // <-- THÊM

    // Xử lý trang chủ cho CUSTOMER (đường dẫn "/")
    @GetMapping("/")
    public String customerHomepage(Model model) {

        model.addAttribute("allStations", stationService.getAllStations());
        return "customer/Home";
    }

    // ===== THÊM HÀM MỚI (Xem tất cả tàu) =====
    @GetMapping("/trains/all")
    public String showAllTrains(Model model) {
        List<Train> trains = trainService.getAllTrains();
        model.addAttribute("trains", trains);
        return "customer/all-trains"; // <-- File HTML MỚI
    }
}