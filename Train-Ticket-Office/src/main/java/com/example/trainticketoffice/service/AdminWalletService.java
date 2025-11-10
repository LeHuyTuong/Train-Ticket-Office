package com.example.trainticketoffice.service;

import java.math.BigDecimal;

public interface AdminWalletService {

    /**
     * Lấy số dư hiện tại của túi tiền.
     * Sẽ tạo túi tiền nếu chưa tồn tại.
     */
    BigDecimal getBalance();

    /**
     * Cộng một khoản tiền vào túi tiền (an toàn).
     */
    void addToBalance(BigDecimal amount);

    /**
     * Trừ một khoản tiền từ túi tiền (an toàn).
     */
    void subtractFromBalance(BigDecimal amount);

    /**
     * Hàm nội bộ để đảm bảo túi tiền tồn tại.
     */
    void initializeWallet();
}