package com.example.trainticketoffice.repository;

import com.example.trainticketoffice.model.AdminWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminWalletRepository extends JpaRepository<AdminWallet, Long> {

    // Chúng ta giả định chỉ có MỘT túi tiền trong hệ thống
    Optional<AdminWallet> findFirstByOrderByIdAsc();
}