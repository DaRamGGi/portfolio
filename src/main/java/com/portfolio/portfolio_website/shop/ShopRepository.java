package com.portfolio.portfolio_website.shop;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopRepository extends JpaRepository<ShopEntity, Long> {
    
    // 제목으로 검색 (올바른 명명: sTitle -> sTitleContaining)
    List<ShopEntity> findBysTitleContainingIgnoreCase(String title);
    
    // 최신순 조회 (페이징)
    Page<ShopEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // 할인 상품만 조회
    @Query("SELECT s FROM ShopEntity s WHERE s.sDiscount > 0 ORDER BY s.sDiscount DESC")
    List<ShopEntity> findDiscountedProducts();
    
    // 가격 범위로 검색
    @Query("SELECT s FROM ShopEntity s WHERE s.sPrice BETWEEN :minPrice AND :maxPrice ORDER BY s.sPrice ASC")
    List<ShopEntity> findByPriceRange(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);
}
