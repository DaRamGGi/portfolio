package com.portfolio.portfolio_website.payment;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class PaymentEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "payment_key", unique = true, nullable = false)
    private String paymentKey;  // 토스페이먼츠 결제키
    
    @Column(name = "order_id", nullable = false)
    private String orderId;  // 주문번호
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;  // 결제금액
    
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus = PaymentStatus.READY;
    
    @Column(name = "payment_method")
    private String paymentMethod;  // 결제수단 (카드, 계좌이체 등)
    
    @Column(name = "payment_method_type")
    private String paymentMethodType;  // 세부 결제수단
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;  // 결제 승인 시간
    
    @Column(name = "receipt_url")
    private String receiptUrl;  // 영수증 URL
    
    @Column(name = "failure_code")
    private String failureCode;  // 실패 코드
    
    @Column(name = "failure_reason", length = 500)
    private String failureReason;  // 실패 사유
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public enum PaymentStatus {
        READY,          // 결제 대기
        IN_PROGRESS,    // 결제 진행중
        WAITING_FOR_DEPOSIT,  // 가상계좌 입금 대기
        DONE,           // 결제 완료
        CANCELED,       // 결제 취소
        PARTIAL_CANCELED,  // 부분 취소
        ABORTED,        // 결제 중단
        EXPIRED         // 결제 만료
    }
}
