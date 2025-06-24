package com.portfolio.portfolio_website.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    private String profileImage;

    @Column(nullable = false)
    private String provider;        // google, kakao, naver

    @Column(nullable = false)
    private String providerId;      // OAuth 제공자의 고유 ID

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime lastLoginAt;

    // 로그인 시점 업데이트
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
