package com.portfolio.portfolio_website.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentRequestDto {
    private String paymentKey;
    private String orderId;
    private BigDecimal amount;
}
