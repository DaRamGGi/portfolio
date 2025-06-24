package com.portfolio.portfolio_website.payment.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDto {
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
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime requestedAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private LocalDateTime approvedAt;
    
    private boolean useEscrow;
    private String lastTransactionKey;
    private BigDecimal suppliedAmount;
    private BigDecimal vat;
    private boolean cultureExpense;
    private BigDecimal taxFreeAmount;
    private Integer taxExemptionAmount;
    private Cancels[] cancels;
    private boolean isPartialCancelable;
    private Card card;
    private VirtualAccount virtualAccount;
    private String secret;
    private MobilePhone mobilePhone;
    private GiftCertificate giftCertificate;
    private Transfer transfer;
    private Receipt receipt;
    private Checkout checkout;
    private EasyPay easyPay;
    private String country;
    private Failure failure;
    private CashReceipt cashReceipt;
    private CashReceipts[] cashReceipts;
    private Discount discount;

    @Data
    public static class Cancels {
        private BigDecimal cancelAmount;
        private String cancelReason;
        private BigDecimal taxFreeAmount;
        private Integer taxExemptionAmount;
        private BigDecimal refundableAmount;
        private BigDecimal easyPayDiscountAmount;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private LocalDateTime canceledAt;
        private String transactionKey;
        private String receiptKey;
    }

    @Data
    public static class Card {
        private String issuerCode;
        private String acquirerCode;
        private String number;
        private Integer installmentPlanMonths;
        private String approveNo;
        private boolean useCardPoint;
        private String cardType;
        private String ownerType;
        private String acquireStatus;
        private BigDecimal amount;
    }

    @Data
    public static class VirtualAccount {
        private String accountType;
        private String accountNumber;
        private String bankCode;
        private String customerName;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private LocalDateTime dueDate;
        private String refundStatus;
        private boolean expired;
        private String settlementStatus;
        private RefundReceiveAccount refundReceiveAccount;
    }

    @Data
    public static class RefundReceiveAccount {
        private String bankCode;
        private String accountNumber;
        private String holderName;
    }

    @Data
    public static class MobilePhone {
        private String customerMobilePhone;
        private String settlementStatus;
        private String receiptUrl;
    }

    @Data
    public static class GiftCertificate {
        private String approveNo;
        private String settlementStatus;
    }

    @Data
    public static class Transfer {
        private String bankCode;
        private String settlementStatus;
    }

    @Data
    public static class Receipt {
        private String url;
    }

    @Data
    public static class Checkout {
        private String url;
    }

    @Data
    public static class EasyPay {
        private String provider;
        private BigDecimal amount;
        private BigDecimal discountAmount;
    }

    @Data
    public static class Failure {
        private String code;
        private String message;
    }

    @Data
    public static class CashReceipt {
        private String type;
        private String receiptKey;
        private String issueNumber;
        private String receiptUrl;
        private BigDecimal amount;
        private BigDecimal taxFreeAmount;
    }

    @Data
    public static class CashReceipts {
        private String receiptKey;
        private String orderId;
        private String orderName;
        private String type;
        private String issueNumber;
        private String receiptUrl;
        private String businessNumber;
        private String transactionType;
        private BigDecimal amount;
        private BigDecimal taxFreeAmount;
        private String issueStatus;
        private Failure failure;
        private String customerIdentityNumber;
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
        private LocalDateTime requestedAt;
    }

    @Data
    public static class Discount {
        private BigDecimal amount;
    }
}
