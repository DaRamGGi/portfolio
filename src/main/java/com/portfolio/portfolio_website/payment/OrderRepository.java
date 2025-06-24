package com.portfolio.portfolio_website.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    
    Optional<OrderEntity> findByOrderId(String orderId);
    
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<OrderEntity> findByOrderStatusOrderByCreatedAtDesc(OrderEntity.OrderStatus orderStatus);
    
    // 관리자용: 최근 주문 20건 조회
    List<OrderEntity> findTop20ByOrderByCreatedAtDesc();
    
    // 통계용 메서드들
    long countByOrderStatus(OrderEntity.OrderStatus orderStatus);
    
    @Query("SELECT SUM(o.totalAmount) FROM OrderEntity o WHERE o.orderStatus = :orderStatus")
    BigDecimal sumTotalAmountByOrderStatus(OrderEntity.OrderStatus orderStatus);
    
    boolean existsByOrderId(String orderId);
}
