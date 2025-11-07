package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.Station;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Integer> {

    Optional<Station> findByCode(String code);

    List<Station> findByStatus(Station.Status status);

    boolean existsByCode(String code);
    Optional<Station> findByName(String name);

    // ===== THÊM PHƯƠNG THỨC NÀY ĐỂ TÌM KIẾM VÀ PHÂN TRANG =====
    /**
     * Tìm kiếm Ga theo Tên Ga (name) hoặc Mã Ga (code) - không phân biệt chữ hoa/thường
     * và trả về kết quả dưới dạng Trang (Page).
     */
    Page<Station> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(String nameKeyword, String codeKeyword, Pageable pageable);

    // Phương thức findAll(Pageable pageable) đã có sẵn trong JpaRepository
}