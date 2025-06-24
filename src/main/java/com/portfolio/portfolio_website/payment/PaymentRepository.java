package com.portfolio.portfolio_website.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    
    Optional<PaymentEntity> findByPaymentKey(String paymentKey);
    
    Optional<PaymentEntity> findByOrderId(String orderId);
    
    List<PaymentEntity> findByPaymentStatusOrderByCreatedAtDesc(PaymentEntity.PaymentStatus paymentStatus);
    
    // 관리자용: 최근 결제 20건 조회
    List<PaymentEntity> findTop20ByOrderByCreatedAtDesc();
    
    // 특정 주문 ID 목록에 대한 결제 조회
    List<PaymentEntity> findByOrderIdInOrderByCreatedAtDesc(List<String> orderIds);
    
    boolean existsByPaymentKey(String paymentKey);
}
