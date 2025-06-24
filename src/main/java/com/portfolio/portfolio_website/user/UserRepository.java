package com.portfolio.portfolio_website.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    
    // 이메일로 사용자 찾기
    Optional<UserEntity> findByEmail(String email);
    
    // Provider와 ProviderId로 사용자 찾기 (OAuth 로그인용)
    Optional<UserEntity> findByProviderAndProviderId(String provider, String providerId);
    
    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);
    
    // Provider별 사용자 목록 조회
    List<UserEntity> findByProvider(String provider);
    
    // 커스텀 쿼리: 최근 로그인한 사용자들 조회
    @Query("SELECT u FROM UserEntity u WHERE u.lastLoginAt >= :since")
    List<UserEntity> findRecentlyActiveUsers(@Param("since") LocalDateTime since);
    
    // 이름으로 사용자 검색 (부분 일치)
    List<UserEntity> findByNameContainingIgnoreCase(String name);
}
