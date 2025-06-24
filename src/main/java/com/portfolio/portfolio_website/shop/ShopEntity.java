package com.portfolio.portfolio_website.shop;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "s_no")
    private Long sNo;
    
    @Column(name = "s_title", nullable = false, length = 200)
    private String sTitle;
    
    @Column(name = "s_content", columnDefinition = "TEXT")
    private String sContent;
    
    @Column(name = "s_photo", length = 500)
    private String sPhoto;
    
    // 상품 가격
    @Column(name = "s_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal sPrice;
    
    // 할인율 (0-100)
    @Column(name = "s_discount")
    private Integer sDiscount = 0;
    
    // 시간 관리
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 할인된 가격 계산 메서드
    public BigDecimal getDiscountedPrice() {
        if (sDiscount == null || sDiscount == 0) {
            return sPrice;
        }
        BigDecimal discountAmount = sPrice.multiply(BigDecimal.valueOf(sDiscount)).divide(BigDecimal.valueOf(100));
        return sPrice.subtract(discountAmount);
    }
    
    // 할인 여부 확인
    public boolean hasDiscount() {
        return sDiscount != null && sDiscount > 0;
    }
}
