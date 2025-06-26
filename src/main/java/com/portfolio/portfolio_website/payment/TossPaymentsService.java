package com.portfolio.portfolio_website.payment;

import com.portfolio.portfolio_website.config.TossPaymentsConfig;
import com.portfolio.portfolio_website.payment.dto.PaymentRequestDto;
import com.portfolio.portfolio_website.payment.dto.PaymentResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TossPaymentsService {
    
    private final WebClient tossPaymentsWebClient;
    private final TossPaymentsConfig tossPaymentsConfig;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    
    /**
     * 주문 생성
     */
    public OrderEntity createOrder(String customerName, String customerEmail, String customerPhone, 
                                   String orderName, BigDecimal amount, Long userId) {
        
        String orderId = generateOrderId();
        
        OrderEntity order = new OrderEntity();
        order.setOrderId(orderId);
        order.setUserId(userId);
        order.setCustomerName(customerName);
        order.setCustomerEmail(customerEmail);
        order.setCustomerPhone(customerPhone);
        order.setOrderName(orderName);
        order.setTotalAmount(amount);
        order.setOrderStatus(OrderEntity.OrderStatus.PENDING);
        
        return orderRepository.save(order);
    }
    
    /**
     * 주문 생성과 결제 승인을 하나의 트랜잭션으로 처리
     */
    @Transactional
    public PaymentResponseDto createOrderAndConfirmPayment(String paymentKey, String orderId, BigDecimal amount,
                                                          String customerName, String customerEmail, String customerPhone, String orderName, Long userId) {
        log.info("🔔 주문 생성 및 결제 승인 시작: orderId={}, amount={}", orderId, amount);
        
        try {
            // 1. 먼저 토스페이먼츠 결제 승인 API 호출
            PaymentRequestDto request = PaymentRequestDto.builder()
                    .paymentKey(paymentKey)
                    .orderId(orderId)
                    .amount(amount)
                    .build();
            
            log.info("🌐 토스페이먼츠 API 호출 시작");
            
            PaymentResponseDto response = tossPaymentsWebClient
                    .post()
                    .uri(tossPaymentsConfig.getApi().getConfirmUrl())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResponseDto.class)
                    .onErrorMap(Exception.class, ex -> {
                        log.error("❌ JSON 파싱 오류 상세: {}", ex.getMessage());
                        return new RuntimeException("토스페이먼츠 응답 파싱 실패: " + ex.getMessage(), ex);
                    })
                    .block();
            
            if (response == null) {
                throw new RuntimeException("토스페이먼츠 API 응답이 null입니다");
            }
            
            log.info("✅ 토스페이먼츠 승인 완료: status={}", response.getStatus());
            
            // 2. 결제 승인이 성공하면 주문 생성
            OrderEntity order = new OrderEntity();
            order.setOrderId(orderId);
            order.setUserId(userId);
            order.setCustomerName(customerName);
            order.setCustomerEmail(customerEmail);
            order.setCustomerPhone(customerPhone); // ⭐ 수정: 실제 phone 데이터 사용
            order.setOrderName(orderName);
            order.setTotalAmount(amount);
            order.setOrderStatus(OrderEntity.OrderStatus.PAID); // 바로 PAID 상태로
            
            orderRepository.save(order);
            log.info("✅ 주문 생성 완료: orderId={}", orderId);
            
            // 3. 결제 정보 저장
            savePaymentInfo(response);
            log.info("✅ 결제 정보 저장 완료");
            
            return response;
            
        } catch (WebClientResponseException e) {
            log.error("❌ 토스페이먼츠 API 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // 결제 실패 정보 저장 (주문은 생성하지 않음)
            saveFailedPayment(paymentKey, orderId, amount, e);
            
            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ 주문 생성 및 결제 승인 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 결제 승인 요청 (기존 메서드 - 사용하지 않음)
     */
    public PaymentResponseDto confirmPayment(String paymentKey, String orderId, BigDecimal amount) {
        log.info("🔔 결제 승인 요청: paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);
        
        try {
            // 주문 검증
            OrderEntity order = orderRepository.findByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));
            
            log.info("✅ 주문 검증 완료: orderStatus={}", order.getOrderStatus());
            
            // 주문 금액 검증
            if (order.getTotalAmount().compareTo(amount) != 0) {
                throw new IllegalArgumentException("주문 금액이 일치하지 않습니다: 주문=" + order.getTotalAmount() + ", 요청=" + amount);
            }
            
            // 결제 요청 DTO 생성
            PaymentRequestDto request = PaymentRequestDto.builder()
                    .paymentKey(paymentKey)
                    .orderId(orderId)
                    .amount(amount)
                    .build();
            
            log.info("🌐 토스페이먼츠 API 호출 시작: {}", tossPaymentsConfig.getApi().getBaseUrl());
            
            // 토스페이먼츠 API 호출
            PaymentResponseDto response = tossPaymentsWebClient
                    .post()
                    .uri(tossPaymentsConfig.getApi().getConfirmUrl())
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(PaymentResponseDto.class)
                    .onErrorMap(Exception.class, ex -> {
                        log.error("❌ JSON 파싱 오류 상세: {}", ex.getMessage());
                        return new RuntimeException("토스페이먼츠 응답 파싱 실패: " + ex.getMessage(), ex);
                    })
                    .block();
            
            if (response != null) {
                log.info("✅ 토스페이먼츠 응답 수신: status={}, method={}", response.getStatus(), response.getMethod());
                
                // 결제 정보 저장
                savePaymentInfo(response);
                
                // 주문 상태 업데이트
                order.setOrderStatus(OrderEntity.OrderStatus.PAID);
                orderRepository.save(order);
                
                log.info("🎉 결제 승인 완료: paymentKey={}", paymentKey);
                return response;
            } else {
                throw new RuntimeException("결제 승인 응답이 null입니다");
            }
            
        } catch (WebClientResponseException e) {
            log.error("❌ 토스페이먼츠 API 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            
            // 결제 실패 정보 저장
            saveFailedPayment(paymentKey, orderId, amount, e);
            
            throw new RuntimeException("결제 승인에 실패했습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("❌ 결제 승인 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("결제 승인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 결제 정보 조회
     */
    public PaymentResponseDto getPayment(String paymentKey) {
        log.info("결제 조회: paymentKey={}", paymentKey);
        
        try {
            return tossPaymentsWebClient
                    .get()
                    .uri(tossPaymentsConfig.getApi().getPaymentUrl() + "/" + paymentKey)
                    .retrieve()
                    .bodyToMono(PaymentResponseDto.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("결제 조회 실패: {}", e.getResponseBodyAsString());
            throw new RuntimeException("결제 조회에 실패했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 주문 ID 생성
     */
    private String generateOrderId() {
        return "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
    
    /**
     * 결제 정보 저장
     */
    private void savePaymentInfo(PaymentResponseDto response) {
        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentKey(response.getPaymentKey());
        payment.setOrderId(response.getOrderId());
        payment.setAmount(response.getTotalAmount());
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.valueOf(response.getStatus()));
        payment.setPaymentMethod(response.getMethod());
        
        // ⭐ 추가: paymentMethodType 설정
        payment.setPaymentMethodType(extractPaymentMethodType(response));
        
        // 날짜 파싱 문제 해결: String으로 받은 후 파싱
        try {
            if (response.getApprovedAt() != null) {
                // approvedAt이 String인 경우 현재 시간으로 대체
                payment.setApprovedAt(java.time.LocalDateTime.now());
            }
        } catch (Exception e) {
            log.warn("날짜 파싱 실패, 현재 시간으로 설정: {}", e.getMessage());
            payment.setApprovedAt(java.time.LocalDateTime.now());
        }
        
        if (response.getReceipt() != null) {
            payment.setReceiptUrl(response.getReceipt().getUrl());
        }
        
        paymentRepository.save(payment);
        log.info("✅ 결제 정보 저장 완료: paymentKey={}, method={}, methodType={}", 
                response.getPaymentKey(), response.getMethod(), payment.getPaymentMethodType());
    }
    
    /**
     * 실패한 결제 정보 저장
     */
    private void saveFailedPayment(String paymentKey, String orderId, BigDecimal amount, WebClientResponseException e) {
        PaymentEntity payment = new PaymentEntity();
        payment.setPaymentKey(paymentKey);
        payment.setOrderId(orderId);
        payment.setAmount(amount);
        payment.setPaymentStatus(PaymentEntity.PaymentStatus.ABORTED);
        payment.setFailureCode(String.valueOf(e.getStatusCode().value()));
        payment.setFailureReason(e.getResponseBodyAsString());
        
        paymentRepository.save(payment);
    }
    
    /**
     * 결제 수단 타입 추출
     */
    private String extractPaymentMethodType(PaymentResponseDto response) {
        String method = response.getMethod();
        String methodType = method; // 기본값
        
        try {
            switch (method) {
                case "카드":
                    if (response.getCard() != null) {
                        methodType = response.getCard().getCardType() != null ? 
                            response.getCard().getCardType() : "일반카드";
                        
                        // 할부 정보 추가
                        if (response.getCard().getInstallmentPlanMonths() != null && 
                            response.getCard().getInstallmentPlanMonths() > 0) {
                            methodType += " (" + response.getCard().getInstallmentPlanMonths() + "개월)";
                        }
                    }
                    break;
                    
                case "가상계좌":
                    if (response.getVirtualAccount() != null) {
                        methodType = response.getVirtualAccount().getBankCode() != null ? 
                            getBankName(response.getVirtualAccount().getBankCode()) + " 가상계좌" : "가상계좌";
                    }
                    break;
                    
                case "계좌이체":
                    if (response.getTransfer() != null) {
                        methodType = response.getTransfer().getBankCode() != null ? 
                            getBankName(response.getTransfer().getBankCode()) + " 계좌이체" : "실시간 계좌이체";
                    }
                    break;
                    
                case "휴대폰":
                    methodType = "휴대폰 결제";
                    break;
                    
                case "간편결제":
                    if (response.getEasyPay() != null) {
                        methodType = response.getEasyPay().getProvider() != null ? 
                            response.getEasyPay().getProvider() + " 간편결제" : "간편결제";
                    }
                    break;
                    
                case "상품권":
                    methodType = "상품권 결제";
                    break;
                    
                default:
                    methodType = method;
                    break;
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 결제 수단 타입 추출 실패, 기본값 사용: method={}, error={}", method, e.getMessage());
            methodType = method;
        }
        
        log.info("🔍 결제 수단 타입 추출: method={} → methodType={}", method, methodType);
        return methodType;
    }
    
    /**
     * 은행 코드를 은행명으로 변환
     */
    private String getBankName(String bankCode) {
        switch (bankCode) {
            case "39": return "경남은행";
            case "34": return "광주은행";
            case "04": return "국민은행";
            case "11": return "농협은행";
            case "31": return "대구은행";
            case "32": return "부산은행";
            case "02": return "산업은행";
            case "45": return "새마을금고";
            case "07": return "수협은행";
            case "88": return "신한은행";
            case "48": return "신협";
            case "27": return "씨티은행";
            case "20": return "우리은행";
            case "71": return "우체국";
            case "81": return "하나은행";
            case "54": return "HSBC";
            case "03": return "기업은행";
            case "06": return "국민은행";
            case "05": return "외환은행";
            case "90": return "카카오뱅크";
            case "89": return "케이뱅크";
            case "92": return "토스뱅크";
            default: return bankCode;
        }
    }
    
    /**
     * 주문 조회
     */
    public OrderEntity getOrder(String orderId) {
        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 주문입니다: " + orderId));
    }
    
    /**
     * 결제 내역 조회 (DB에서)
     */
    public PaymentEntity getPaymentFromDb(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 결제입니다: " + paymentKey));
    }
}
