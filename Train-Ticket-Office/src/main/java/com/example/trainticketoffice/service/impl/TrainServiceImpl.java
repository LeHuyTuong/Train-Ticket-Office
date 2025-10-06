package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.Train;
import com.example.trainticketoffice.repository.TrainRepository;
import com.example.trainticketoffice.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TrainServiceImpl implements TrainService {

    private final TrainRepository trainRepository;

    @Autowired
    public TrainServiceImpl(TrainRepository trainRepository) {
        this.trainRepository = trainRepository;
    }

    @Override
    public Train createTrain(Train train) {
        // Có thể thêm logic kiểm tra trước khi lưu, ví dụ: tên tàu không được trùng
        return trainRepository.save(train);
    }

    @Override
    public List<Train> getAllTrains() {
        return trainRepository.findAll();
    }

    @Override
    public Optional<Train> getTrainById(int trainId) {
        return trainRepository.findById(trainId);
    }

    @Override
    public Train updateTrain(int trainId, Train trainDetails) {
        // Tìm train hiện có trong DB
        Train existingTrain = trainRepository.findById(trainId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tàu với ID: " + trainId));

        // Cập nhật thông tin từ trainDetails vào existingTrain
        existingTrain.setName(trainDetails.getName());
        existingTrain.setRoute(trainDetails.getRoute());
        // Lưu ý: Việc cập nhật danh sách Toa tàu (carriages) cần xử lý cẩn thận hơn
        // ở đây chỉ cập nhật thông tin cơ bản của Train

        return trainRepository.save(existingTrain);
    }

    @Override
    public void deleteTrain(int trainId) {
        // Kiểm tra xem tàu có tồn tại không trước khi xóa
        if (!trainRepository.existsById(trainId)) {
            throw new RuntimeException("Không tìm thấy tàu với ID: " + trainId);
        }
        trainRepository.deleteById(trainId);
    }
}
