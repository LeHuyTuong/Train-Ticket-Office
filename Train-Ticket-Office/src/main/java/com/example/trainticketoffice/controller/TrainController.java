package com.example.trainticketoffice.controller;

import com.example.trainticketoffice.model.Train;
import com.example.trainticketoffice.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trains")
public class TrainController {

    private final TrainService trainService;

    @Autowired
    public TrainController(TrainService trainService) {
        this.trainService = trainService;
    }

    /**
     * API để tạo một tàu mới.
     * POST /api/trains
     * @param train Dữ liệu của tàu mới trong body của request.
     * @return Tàu đã được tạo và lưu vào DB.
     */
    @PostMapping
    public Train createTrain(@RequestBody Train train) {
        return trainService.createTrain(train);
    }

    /**
     * API để lấy danh sách tất cả các tàu.
     * GET /api/trains
     * @return Danh sách tất cả các tàu.
     */
    @GetMapping
    public List<Train> getAllTrains() {
        return trainService.getAllTrains();
    }

    /**
     * API để lấy thông tin một tàu cụ thể bằng ID.
     * GET /api/trains/{trainId}
     * @param trainId ID của tàu cần tìm.
     * @return Trả về tàu nếu tìm thấy (200 OK), ngược lại trả về 404 Not Found.
     */
    @GetMapping("/{trainId}")
    public ResponseEntity<Train> getTrainById(@PathVariable int trainId) {
        return trainService.getTrainById(trainId)
                .map(ResponseEntity::ok) // Nếu tìm thấy, trả về 200 OK với body là train
                .orElse(ResponseEntity.notFound().build()); // Nếu không tìm thấy, trả về 404 Not Found
    }

    /**
     * API để cập nhật thông tin một tàu đã có.
     * PUT /api/trains/{trainId}
     * @param trainId ID của tàu cần cập nhật.
     * @param trainDetails Thông tin mới của tàu trong body request.
     * @return Tàu với thông tin đã được cập nhật.
     */
    @PutMapping("/{trainId}")
    public ResponseEntity<Train> updateTrain(@PathVariable int trainId, @RequestBody Train trainDetails) {
        try {
            Train updatedTrain = trainService.updateTrain(trainId, trainDetails);
            return ResponseEntity.ok(updatedTrain);
        } catch (RuntimeException e) {
            // Service sẽ throw exception nếu không tìm thấy tàu, bắt lại và trả về 404
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * API để xóa một tàu bằng ID.
     * DELETE /api/trains/{trainId}
     * @param trainId ID của tàu cần xóa.
     * @return Trả về 204 No Content nếu xóa thành công.
     */
    @DeleteMapping("/{trainId}")
    public ResponseEntity<Void> deleteTrain(@PathVariable int trainId) {
        try {
            trainService.deleteTrain(trainId);
            return ResponseEntity.noContent().build(); // Trả về 204 No Content - là tiêu chuẩn cho API xóa thành công
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
