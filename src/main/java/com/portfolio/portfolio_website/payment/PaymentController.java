package com.portfolio.portfolio_website.payment;

import com.portfolio.portfolio_website.config.TossPaymentsConfig;
import com.portfolio.portfolio_website.payment.dto.OrderHistoryDto;
import com.portfolio.portfolio_website.payment.dto.PaymentResponseDto;
import com.portfolio.portfolio_website.shop.ShopEntity;
import com.portfolio.portfolio_website.shop.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/project/payment")
public class PaymentController {
    
    private final TossPaymentsService tossPaymentsService;
    private final TossPaymentsConfig tossPaymentsConfig;
    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    
    /**
     * 결제 페이지 (상품 구매)
     */
    @GetMapping("/checkout/{sNo}")
    public String checkoutPage(@PathVariable Long sNo, Model model, HttpSession session) {
        log.info("결제 페이지 접근: 상품번호={}", sNo);
        
        try {
            // 상품 정보 조회
            ShopEntity product = shopRepository.findById(sNo)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 상품입니다"));
            
            // 할인가 계산
            BigDecimal finalPrice = product.getSPrice();
            if (product.getSDiscount() > 0) {
                BigDecimal discountAmount = finalPrice.multiply(BigDecimal.valueOf(product.getSDiscount()))
                        .divide(BigDecimal.valueOf(100));
                finalPrice = finalPrice.subtract(discountAmount);
            }
            
            // 주문 ID 생성
            String orderId = generateOrderId();
            
            // 세션에서 사용자 정보 가져오기 (안전한 방식)
            Long userId = null;
            String userName = null;
            String userEmail = null;
            Boolean isAdmin = null;
            
            try {
                Object userIdObj = session.getAttribute("userId");
                Object userNameObj = session.getAttribute("userName");
                Object userEmailObj = session.getAttribute("userEmail");
                isAdmin = (Boolean) session.getAttribute("isAdmin");
                
                log.info("🛒 체크아웃 페이지 - 세션 원본 데이터:");
                log.info("  - userIdObj: {} (타입: {})", userIdObj, userIdObj != null ? userIdObj.getClass().getSimpleName() : "null");
                log.info("  - userNameObj: {} (타입: {})", userNameObj, userNameObj != null ? userNameObj.getClass().getSimpleName() : "null");
                log.info("  - userEmailObj: {} (타입: {})", userEmailObj, userEmailObj != null ? userEmailObj.getClass().getSimpleName() : "null");
                log.info("  - isAdmin: {}", isAdmin);
                
                // 안전한 타입 변환
                if (userIdObj instanceof Long) {
                    userId = (Long) userIdObj;
                } else if (userIdObj instanceof Integer) {
                    userId = ((Integer) userIdObj).longValue();
                }
                
                if (userNameObj instanceof String) {
                    userName = (String) userNameObj;
                }
                
                if (userEmailObj instanceof String) {
                    userEmail = (String) userEmailObj;
                }
                
                // 관리자 로그인인 경우도 체크
                if (isAdmin != null && isAdmin) {
                    Object adminNameObj = session.getAttribute("adminName");
                    Object adminEmailObj = session.getAttribute("adminEmail");
                    
                    if (adminNameObj instanceof String && adminNameObj != null) {
                        userName = (String) adminNameObj;
                    }
                    if (adminEmailObj instanceof String && adminEmailObj != null) {
                        userEmail = (String) adminEmailObj;
                    }
                }
                
            } catch (Exception e) {
                log.error("❌ 체크아웃 페이지 세션 정보 처리 중 오류: {}", e.getMessage(), e);
            }
            
            log.info("🛒 체크아웃 페이지 최종 사용자 정보:");
            log.info("  - userId: {}", userId);
            log.info("  - userName: '{}'", userName);
            log.info("  - userEmail: '{}'", userEmail);
            log.info("  - isAdmin: {}", isAdmin);
            
            // 세션에 주문 정보 임시 저장
            session.setAttribute("tempOrderId", orderId);
            session.setAttribute("tempProductId", sNo);
            session.setAttribute("tempAmount", finalPrice);
            
            // ⭐ 추가: phone 정보도 세션에 저장할 수 있도록 준비
            session.setAttribute("tempCustomerName", userName != null ? userName : "");
            session.setAttribute("tempCustomerEmail", userEmail != null ? userEmail : "");
            
            model.addAttribute("product", product);
            model.addAttribute("finalPrice", finalPrice);
            model.addAttribute("orderId", orderId);
            model.addAttribute("clientKey", tossPaymentsConfig.getClientKey());
            model.addAttribute("successUrl", tossPaymentsConfig.getSuccessUrl());
            model.addAttribute("failUrl", tossPaymentsConfig.getFailUrl());
            
            // 사용자 정보를 결제 폼에 전달
            model.addAttribute("customerName", userName != null ? userName : "");
            model.addAttribute("customerEmail", userEmail != null ? userEmail : "");
            model.addAttribute("isLoggedIn", userId != null || (isAdmin != null && isAdmin));
            
            // HomeController와 동일한 세션 정보 추가 (네비게이션 바용)
            addSessionInfoToModel(model, session);
            
            return "payment/checkout";
            
        } catch (Exception e) {
            log.error("결제 페이지 로드 실패: {}", e.getMessage());
            return "redirect:/project/payment?error=checkout_failed";
        }
    }
    
    /**
     * 세션 정보를 모델에 추가하는 공통 메서드 (HomeController와 동일)
     */
    private void addSessionInfoToModel(Model model, HttpSession session) {
        // OAuth2 로그인 정보
        Long userId = (Long) session.getAttribute("userId");
        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");
        Boolean isOAuthLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        
        // 관리자 로그인 정보
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        String adminName = (String) session.getAttribute("adminName");
        String adminEmail = (String) session.getAttribute("adminEmail");
        
        // 로그인 상태 결정
        boolean isOAuth2Logged = (userId != null && isOAuthLoggedIn != null && isOAuthLoggedIn);
        boolean isAdminLogged = (isAdmin != null && isAdmin);
        boolean isLoggedIn = isOAuth2Logged || isAdminLogged;
        
        // 표시할 사용자 정보
        String displayName = isAdminLogged ? 
            (adminName != null ? adminName : "관리자") : 
            (userName != null ? userName : "사용자");
        
        String displayEmail = isAdminLogged ? adminEmail : userEmail;
        
        // 모델에 추가
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isOAuth2User", isOAuth2Logged);
        model.addAttribute("isAdminUser", isAdminLogged);
        model.addAttribute("userName", displayName);
        model.addAttribute("userEmail", displayEmail);
        model.addAttribute("userId", userId);
        
        log.debug("PaymentController 세션 정보 추가: isLoggedIn={}, isOAuth2={}, isAdmin={}, name={}", 
                 isLoggedIn, isOAuth2Logged, isAdminLogged, displayName);
    }
    
    /**
     * 결제 성공 처리
     */
    @GetMapping("/success")
    public String paymentSuccess(@RequestParam String paymentKey,
                                @RequestParam String orderId,
                                @RequestParam BigDecimal amount,
                                Model model,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        
        log.info("🎉 결제 성공 콜백 수신: paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);
        
        try {
            // 세션에서 임시 저장된 주문 정보 확인
            String tempOrderId = (String) session.getAttribute("tempOrderId");
            BigDecimal tempAmount = (BigDecimal) session.getAttribute("tempAmount");
            
            log.info("📦 세션 주문 정보: tempOrderId={}, tempAmount={}", tempOrderId, tempAmount);
            
            if (tempOrderId == null) {
                throw new IllegalArgumentException("❌ 세션에 주문 정보가 없습니다");
            }
            
            if (!orderId.equals(tempOrderId)) {
                throw new IllegalArgumentException("❌ 주문 ID가 일치하지 않습니다: 요청=" + orderId + ", 세션=" + tempOrderId);
            }
            
            if (tempAmount == null || tempAmount.compareTo(amount) != 0) {
                throw new IllegalArgumentException("❌ 결제 금액이 일치하지 않습니다: 요청=" + amount + ", 세션=" + tempAmount);
            }
            
            log.info("✅ 주문 정보 검증 완료");
            
            // 주문자 정보 준비 - 세션에서 가져오기 (더 안전한 방식)
            Long userId = null;
            String customerName = null;
            String customerEmail = null;
            
            try {
                // 세션에서 사용자 정보 가져오기
                Object userIdObj = session.getAttribute("userId");
                Object userNameObj = session.getAttribute("userName");
                Object userEmailObj = session.getAttribute("userEmail");
                Boolean isLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
                
                log.info("🔍 세션에서 가져온 원본 데이터:");
                log.info("  - userIdObj: {} (타입: {})", userIdObj, userIdObj != null ? userIdObj.getClass().getSimpleName() : "null");
                log.info("  - userNameObj: {} (타입: {})", userNameObj, userNameObj != null ? userNameObj.getClass().getSimpleName() : "null");
                log.info("  - userEmailObj: {} (타입: {})", userEmailObj, userEmailObj != null ? userEmailObj.getClass().getSimpleName() : "null");
                log.info("  - isLoggedIn: {}", isLoggedIn);
                
                // 안전한 타입 변환
                if (userIdObj instanceof Long) {
                    userId = (Long) userIdObj;
                } else if (userIdObj instanceof Integer) {
                    userId = ((Integer) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    try {
                        userId = Long.parseLong((String) userIdObj);
                    } catch (NumberFormatException e) {
                        log.warn("userId 문자열 변환 실패: {}", userIdObj);
                    }
                }
                
                if (userNameObj instanceof String) {
                    customerName = (String) userNameObj;
                }
                
                if (userEmailObj instanceof String) {
                    customerEmail = (String) userEmailObj;
                }
                
                // 관리자 로그인인 경우도 체크
                Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
                if (isAdmin != null && isAdmin) {
                    Object adminNameObj = session.getAttribute("adminName");
                    Object adminEmailObj = session.getAttribute("adminEmail");
                    
                    if (adminNameObj instanceof String && adminNameObj != null) {
                        customerName = (String) adminNameObj;
                    }
                    if (adminEmailObj instanceof String && adminEmailObj != null) {
                        customerEmail = (String) adminEmailObj;
                    }
                    
                    log.info("🔑 관리자 로그인 정보:");
                    log.info("  - adminName: {}", adminNameObj);
                    log.info("  - adminEmail: {}", adminEmailObj);
                }
                
            } catch (Exception e) {
                log.error("❌ 세션 정보 처리 중 오류: {}", e.getMessage(), e);
            }
            
            log.info("🔍 최종 처리된 주문자 정보:");
            log.info("  - userId: {}", userId);
            log.info("  - customerName: '{}'", customerName);
            log.info("  - customerEmail: '{}'", customerEmail);
            
            // 기본값 설정 (세션에 정보가 없는 경우에만)
            if (customerName == null || customerName.trim().isEmpty()) {
                customerName = "구매자";
                log.warn("⚠️ 세션에 사용자명이 없어 기본값 사용: '{}'", customerName);
            }
            if (customerEmail == null || customerEmail.trim().isEmpty()) {
                customerEmail = "guest@example.com";
                log.warn("⚠️ 세션에 이메일이 없어 기본값 사용: '{}'", customerEmail);
            }
            
            // ⭐ 추가: 세션에서 customerPhone 가져오기
            String customerPhone = (String) session.getAttribute("tempCustomerPhone");
            
            log.info("🎯 결제에 사용될 최종 정보:");
            log.info("  - userId: {}", userId);
            log.info("  - customerName: '{}'", customerName);
            log.info("  - customerEmail: '{}'", customerEmail);
            log.info("  - customerPhone: '{}'", customerPhone); // 추가
            
            Long productId = (Long) session.getAttribute("tempProductId");
            ShopEntity product = shopRepository.findById(productId).orElse(null);
            String orderName = (product != null) ? product.getSTitle() : "상품 구매";
            
            log.info("🛒 주문 생성 및 결제 승인 시작");
            
            // 🔧 수정: 주문 생성과 결제 승인을 하나의 메서드로 처리 (phone 추가)
            PaymentResponseDto paymentResponse = tossPaymentsService.createOrderAndConfirmPayment(
                    paymentKey, orderId, amount, customerName, customerEmail, customerPhone, orderName, userId);
            
            // 세션 정리
            session.removeAttribute("tempOrderId");
            session.removeAttribute("tempProductId");
            session.removeAttribute("tempAmount");
            session.removeAttribute("tempCustomerName");
            session.removeAttribute("tempCustomerEmail");
            session.removeAttribute("tempCustomerPhone"); // ⭐ 추가
            
            // 주문 정보 다시 조회 (결제 승인 후)
            OrderEntity order = tossPaymentsService.getOrder(orderId);
            
            model.addAttribute("payment", paymentResponse);
            model.addAttribute("order", order);
            
            log.info("🎉 결제 처리 완료!");
            return "payment/success";
            
        } catch (Exception e) {
            log.error("❌ 결제 처리 실패: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "결제 승인에 실패했습니다: " + e.getMessage());
            return "redirect:/project/payment/fail";
        }
    }
    
    /**
     * 결제 실패 처리
     */
    @GetMapping("/fail")
    public String paymentFail(@RequestParam(required = false) String code,
                             @RequestParam(required = false) String message,
                             @RequestParam(required = false) String orderId,
                             Model model,
                             HttpSession session) {
        
        log.error("결제 실패: code={}, message={}, orderId={}", code, message, orderId);
        
        // 세션 정리
        session.removeAttribute("tempOrderId");
        session.removeAttribute("tempProductId");
        session.removeAttribute("tempAmount");
        session.removeAttribute("tempCustomerName");
        session.removeAttribute("tempCustomerEmail");
        session.removeAttribute("tempCustomerPhone"); // ⭐ 추가
        
        model.addAttribute("errorCode", code);
        model.addAttribute("errorMessage", message);
        model.addAttribute("orderId", orderId);
        
        return "payment/fail";
    }
    
    /**
     * 결제 내역 조회
     */
    @GetMapping("/history")
    public String paymentHistory(Model model, HttpSession session) {
        log.info("📋 주문내역 페이지 접근");
        
        // OAuth2 로그인 확인
        Long userId = (Long) session.getAttribute("userId");
        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");
        Boolean isOAuthLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        
        // 관리자 로그인 확인 
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        String adminName = (String) session.getAttribute("adminName");
        String adminEmail = (String) session.getAttribute("adminEmail");
        
        log.info("🔍 로그인 상태 확인:");
        log.info("   - OAuth2: userId={}, userName={}, isOAuthLoggedIn={}", userId, userName, isOAuthLoggedIn);
        log.info("   - Admin: isAdmin={}, adminName={}, adminEmail={}", isAdmin, adminName, adminEmail);
        
        // 로그인 확인: OAuth2 또는 관리자 로그인 필요
        boolean isOAuth2Logged = (userId != null && isOAuthLoggedIn != null && isOAuthLoggedIn);
        boolean isAdminLogged = (isAdmin != null && isAdmin);
        boolean isLoggedIn = isOAuth2Logged || isAdminLogged;
        
        if (!isLoggedIn) {
            log.warn("❌ 비로그인 사용자의 주문내역 접근 시도");
            model.addAttribute("errorMessage", "주문내역을 보려면 로그인이 필요합니다.");
            return "redirect:/login?error=login_required&returnUrl=/project/payment/history";
        }
        
        try {
            List<OrderEntity> orders;
            List<PaymentEntity> payments;
            String displayName;
            String displayEmail;
            
            if (isAdminLogged) {
                // 관리자: 모든 주문 내역 조회
                log.info("🔑 관리자 권한으로 전체 주문내역 조회: {}", adminName);
                orders = orderRepository.findTop20ByOrderByCreatedAtDesc();
                payments = paymentRepository.findTop20ByOrderByCreatedAtDesc();
                displayName = adminName != null ? adminName : "관리자";
                displayEmail = adminEmail;
                
                // 관리자 통계 계산
                long completedOrders = orderRepository.countByOrderStatus(OrderEntity.OrderStatus.PAID);
                long pendingOrders = orderRepository.countByOrderStatus(OrderEntity.OrderStatus.PENDING);
                BigDecimal totalRevenue = orderRepository.sumTotalAmountByOrderStatus(OrderEntity.OrderStatus.PAID);
                
                model.addAttribute("isAdminView", true);
                model.addAttribute("completedOrders", completedOrders);
                model.addAttribute("pendingOrders", pendingOrders);
                model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            } else {
                // 일반 사용자: 본인 주문만 조회
                log.info("👤 사용자 주문내역 조회: userId={}, userName={}", userId, userName);
                orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
                payments = paymentRepository.findByOrderIdInOrderByCreatedAtDesc(
                    orders.stream().map(OrderEntity::getOrderId).toList()
                );
                displayName = userName != null ? userName : "사용자";
                displayEmail = userEmail;
                model.addAttribute("isAdminView", false);
            }
            
            // 주문과 결제 정보 매핑
            Map<String, PaymentEntity> paymentMap = payments.stream()
                .collect(Collectors.toMap(PaymentEntity::getOrderId, p -> p, (existing, replacement) -> existing));
            
            // 주문 정보와 결제 정보를 조합한 DTO 생성
            List<OrderHistoryDto> orderHistory = orders.stream()
                .map(order -> {
                    PaymentEntity payment = paymentMap.get(order.getOrderId());
                    return OrderHistoryDto.builder()
                        .order(order)
                        .payment(payment)
                        .build();
                })
                .toList();
            
            model.addAttribute("orderHistory", orderHistory);
            model.addAttribute("userName", displayName);
            model.addAttribute("userEmail", displayEmail);
            model.addAttribute("totalOrders", orders.size());
            model.addAttribute("isOAuth2User", isOAuth2Logged);
            model.addAttribute("isAdminUser", isAdminLogged);
            
            log.info("✅ 주문내역 조회 완료: {}건", orders.size());
            return "payment/history";
            
        } catch (Exception e) {
            log.error("❌ 주문내역 조회 실패: {}", e.getMessage(), e);
            model.addAttribute("error", "주문내역을 불러오는데 실패했습니다.");
            return "payment/history";
        }
    }
    
    /**
     * 관리자 결제 관리
     */
    @GetMapping("/admin")
    public String paymentAdmin(Model model, HttpSession session) {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login?error=admin_required";
        }
        
        // TODO: 관리자 결제 관리 로직 구현
        
        return "payment/admin";
    }
    
    /**
     * 고객 정보 세션 저장 API (결제 전 호출)
     */
    @PostMapping("/save-customer-info")
    @ResponseBody
    public Map<String, String> saveCustomerInfo(@RequestBody Map<String, String> customerInfo, HttpSession session) {
        log.info("📝 고객 정보 세션 저장: {}", customerInfo);
        
        try {
            String customerName = customerInfo.get("customerName");
            String customerEmail = customerInfo.get("customerEmail");
            String customerPhone = customerInfo.get("customerPhone");
            
            // 세션에 고객 정보 저장
            session.setAttribute("tempCustomerName", customerName);
            session.setAttribute("tempCustomerEmail", customerEmail);
            session.setAttribute("tempCustomerPhone", customerPhone);
            
            log.info("✅ 고객 정보 세션 저장 완료: name={}, email={}, phone={}", 
                    customerName, customerEmail, customerPhone);
            
            return Map.of("status", "success", "message", "고객 정보 저장 완료");
            
        } catch (Exception e) {
            log.error("❌ 고객 정보 세션 저장 실패: {}", e.getMessage());
            return Map.of("status", "error", "message", "고객 정보 저장 실패");
        }
    }
    
    /**
     * 주문 ID 생성
     */
    private String generateOrderId() {
        return "ORDER_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}
