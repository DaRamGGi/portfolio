package com.portfolio.portfolio_website.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SimplePaymentResponseDto {
    private String version;
    private String paymentKey;
    private String type;
    private String orderId;
    private String orderName;
    private String mId;
    private String currency;
    private String method;
    private BigDecimal totalAmount;
    private BigDecimal balanceAmount;
    private String status;
    
    // 날짜를 String으로 받아서 파싱 문제 해결
    private String requestedAt;
    private String approvedAt;
    
    private boolean useEscrow;
    private String lastTransactionKey;
    private BigDecimal suppliedAmount;
    private BigDecimal vat;
    private boolean cultureExpense;
    private BigDecimal taxFreeAmount;
    private Integer taxExemptionAmount;
    
    // 필요한 중첩 클래스들만 간단하게
    @Data
    public static class Receipt {
        private String url;
    }
    
    @Data
    public static class Failure {
        private String code;
        private String message;
    }
    
    private Receipt receipt;
    private Failure failure;
}
