package com.example.trainticketoffice.model;


import com.example.trainticketoffice.common.PaymentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal; // <-- THÊM
import java.time.LocalDateTime;
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    // ===== SỬA TỪ Booking SANG Order =====
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false) // Đổi từ booking_id sang order_id
    private Order order;
    // ===================================

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ===== SỬA TỪ double =====
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_ref", unique = true, length = 64)
    private String transactionRef;

    @Column(name = "order_info")
    private String orderInfo;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "bank_tran_no")
    private String bankTranNo;

    @Column(name = "vnp_transaction_no")
    private String vnpTransactionNo;

    @Column(name = "response_code")
    private String responseCode;

    @Column(name = "pay_date")
    private LocalDateTime payDate;

    @Column(name = "secure_hash", length = 256)
    private String secureHash;
}