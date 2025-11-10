package com.example.trainticketoffice.service.impl;

import com.example.trainticketoffice.model.AdminWallet;
import com.example.trainticketoffice.repository.AdminWalletRepository;
import com.example.trainticketoffice.service.AdminWalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminWalletServiceImpl implements AdminWalletService {

    private final AdminWalletRepository adminWalletRepository;

    // Dùng object này để khóa (lock) khi cập nhật số dư,
    // tránh trường hợp 2 giao dịch cập nhật cùng lúc.
    private static final Object walletLock = new Object();

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance() {
        AdminWallet wallet = getOrCreateWallet();
        return wallet.getBalance();
    }

    @Override
    @Transactional
    public void addToBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // Không cộng số âm hoặc 0
        }

        synchronized (walletLock) {
            AdminWallet wallet = getOrCreateWallet();
            wallet.setBalance(wallet.getBalance().add(amount));
            adminWalletRepository.save(wallet);
        }
    }

    @Override
    @Transactional
    public void subtractFromBalance(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // Không trừ số âm hoặc 0
        }

        synchronized (walletLock) {
            AdminWallet wallet = getOrCreateWallet();
            // Cho phép số dư âm nếu admin refund nhiều hơn số dư hiện có
            wallet.setBalance(wallet.getBalance().subtract(amount));
            adminWalletRepository.save(wallet);
        }
    }

    @Override
    @Transactional
    public void initializeWallet() {
        // Gọi hàm này để đảm bảo ví được tạo
        getOrCreateWallet();
    }

    private AdminWallet getOrCreateWallet() {
        Optional<AdminWallet> walletOpt = adminWalletRepository.findFirstByOrderByIdAsc();
        if (walletOpt.isPresent()) {
            return walletOpt.get();
        } else {
            // Nếu chưa có, tạo mới với số dư 0
            AdminWallet newWallet = new AdminWallet(BigDecimal.ZERO);
            return adminWalletRepository.save(newWallet);
        }
    }
}