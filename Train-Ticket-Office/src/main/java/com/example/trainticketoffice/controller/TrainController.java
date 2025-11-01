package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Train;
import com.example.trainticketoffice.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/trains") // URL chính để quản lý tàu, ví dụ: http://localhost:8080/trains
public class TrainController {

    private final TrainService trainService;

    @Autowired
    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    /**
     * Hiển thị trang danh sách tất cả các tàu.
     * Tương ứng với chức năng Read (All).
     */
    @GetMapping
    public String listTrains(Model model) {
        List<Train> trainList = trainService.getAllTrains();
        model.addAttribute("trains", trainList); // Đưa danh sách tàu vào model để HTML sử dụng
        return "train/list"; // Trả về file: templates/train/list.html
    }

    /**
     * Hiển thị form để tạo một tàu mới.
     */
    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("train", new Train()); // Tạo một đối tượng Train rỗng cho form
        return "train/form"; // Trả về file: templates/train/form.html
    }

    /**
     * Hiển thị form để chỉnh sửa một tàu đã có, dựa vào ID.
     * Tương ứng với chức năng Read (by ID) để lấy dữ liệu cho form Update.
     */
    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") int id, Model model) {
        Optional<Train> trainOptional = trainService.getTrainById(id);
        if (trainOptional.isPresent()) {
            model.addAttribute("train", trainOptional.get()); // Gửi tàu tìm được vào form
            return "train/form"; // Tái sử dụng cùng một form cho cả Thêm và Sửa
        }
        // Nếu không tìm thấy tàu, chuyển hướng về trang danh sách
        return "redirect:/trains";
    }

    /**
     * Xử lý dữ liệu từ form gửi lên để lưu (cả tạo mới và cập nhật).
     * Tương ứng với chức năng Create và Update.
     */
    @PostMapping("/save")
    public String saveTrain(@ModelAttribute("train") Train train) {
        // @ModelAttribute sẽ tự động lấy dữ liệu từ form và gán vào đối tượng 'train'

        // Kiểm tra xem train đã có ID chưa.
        // Nếu có (trainId != 0), thì cập nhật.
        // Nếu không có, tạo mới.
        if (train.getTrainId() != 0) {
            trainService.updateTrain(train.getTrainId(), train);
        } else {
            trainService.createTrain(train);
        }

        // Sau khi lưu xong, chuyển hướng về trang danh sách
        return "redirect:/trains";
    }

    /**
     * Xử lý yêu cầu xóa một tàu dựa vào ID.
     * Tương ứng với chức năng Delete.
     */
    @GetMapping("/delete/{id}")
    public String deleteTrain(@PathVariable("id") int id) {
        try {
            trainService.deleteTrain(id);
        } catch (RuntimeException e) {
            System.err.println("Lỗi khi xóa tàu ID " + id + ": " + e.getMessage());
        }
        return "redirect:/trains";
    }
}
