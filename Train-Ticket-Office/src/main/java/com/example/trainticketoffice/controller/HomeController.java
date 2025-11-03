package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final StationService stationService;

    // Xử lý trang chủ cho CUSTOMER (đường dẫn "/")
    @GetMapping("/")
    public String customerHomepage(Model model) {

        // SỬA LỖI Ở ĐÂY: Đổi 'findAll()' thành 'getAllStations()'
        model.addAttribute("allStations", stationService.getAllStations());

        return "customer/Home"; // Trỏ đến file home.html
    }
}