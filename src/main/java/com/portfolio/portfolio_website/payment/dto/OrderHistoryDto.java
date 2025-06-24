package com.portfolio.portfolio_website.payment.dto;

import com.portfolio.portfolio_website.payment.OrderEntity;
import com.portfolio.portfolio_website.payment.PaymentEntity;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class OrderHistoryDto {
    private OrderEntity order;
    private PaymentEntity payment;
    
    // 편의 메서드들
    public String getOrderId() {
        return order != null ? order.getOrderId() : null;
    }
    
    public String getOrderName() {
        return order != null ? order.getOrderName() : null;
    }
    
    public String getCustomerName() {
        return order != null ? order.getCustomerName() : null;
    }
    
    public BigDecimal getTotalAmount() {
        return order != null ? order.getTotalAmount() : null;
    }
    
    public OrderEntity.OrderStatus getOrderStatus() {
        return order != null ? order.getOrderStatus() : null;
    }
    
    public LocalDateTime getOrderDate() {
        return order != null ? order.getCreatedAt() : null;
    }
    
    public String getPaymentKey() {
        return payment != null ? payment.getPaymentKey() : null;
    }
    
    public String getPaymentMethod() {
        return payment != null ? payment.getPaymentMethod() : null;
    }
    
    public PaymentEntity.PaymentStatus getPaymentStatus() {
        return payment != null ? payment.getPaymentStatus() : null;
    }
    
    public LocalDateTime getPaymentDate() {
        return payment != null ? payment.getApprovedAt() : null;
    }
    
    public String getReceiptUrl() {
        return payment != null ? payment.getReceiptUrl() : null;
    }
    
    public boolean hasPayment() {
        return payment != null;
    }
    
    public boolean isPaymentCompleted() {
        return payment != null && PaymentEntity.PaymentStatus.DONE.equals(payment.getPaymentStatus());
    }
}
