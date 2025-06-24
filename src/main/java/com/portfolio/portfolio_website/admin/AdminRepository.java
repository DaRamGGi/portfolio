package com.portfolio.portfolio_website.admin;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<AdminEntity, Long> {
    
    // 이메일로 관리자 찾기 (로그인용)
    Optional<AdminEntity> findByEmail(String email);
    
    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);
    
    // 이름으로 관리자 검색
    List<AdminEntity> findByNameContainingIgnoreCase(String name);
    
    // 커스텀 쿼리: 최근 로그인한 관리자들 조회
    @Query("SELECT a FROM AdminEntity a WHERE a.lastLoginAt >= :since")
    List<AdminEntity> findRecentlyActiveAdmins(@Param("since") LocalDateTime since);
    
    // 생성일 기준 정렬된 관리자 목록
    List<AdminEntity> findAllByOrderByCreatedAtDesc();
}
