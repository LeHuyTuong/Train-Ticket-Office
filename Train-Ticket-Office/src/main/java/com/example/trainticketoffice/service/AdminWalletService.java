package com.example.trainticketoffice.service;

import java.math.BigDecimal;

public interface AdminWalletService {

    BigDecimal getBalance();
    void addToBalance(BigDecimal amount);
    void subtractFromBalance(BigDecimal amount);
    void initializeWallet();
}