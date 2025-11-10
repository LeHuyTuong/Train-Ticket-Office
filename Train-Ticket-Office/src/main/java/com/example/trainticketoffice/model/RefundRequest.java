package com.example.trainticketoffice.model;

import com.example.trainticketoffice.common.RefundStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refund_requests")
@Data
@EqualsAndHashCode(callSuper = true)
public class RefundRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết 1-1 với Vé (Booking)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, columnDefinition = "nvarchar(100)")
    private String bankName; // Tên ngân hàng

    @Column(nullable = false, columnDefinition = "nvarchar(50)")
    private String bankAccountNumber; // Số tài khoản

    @Column(nullable = false, columnDefinition = "nvarchar(100)")
    private String accountHolderName; // Tên chủ tài khoản

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    @UpdateTimestamp
    private LocalDateTime processedAt;
}