package com.portfolio.portfolio_website.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    
    // 이메일로 관리자 찾기
    public AdminEntity findByEmail(String email) {
        return adminRepository.findByEmail(email).orElse(null);
    }
    
    // 관리자 로그인 검증
    public AdminEntity authenticate(String email, String password) {
        AdminEntity admin = findByEmail(email);
        if (admin != null && passwordEncoder.matches(password, admin.getPassword())) {
            // 마지막 로그인 시간 업데이트
            admin.updateLastLogin();
            adminRepository.save(admin);
            return admin;
        }
        return null;
    }
    
    // 관리자 생성
    public AdminEntity createAdmin(String email, String name, String password) {
        AdminEntity admin = AdminEntity.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode(password))
                .build();
        return adminRepository.save(admin);
    }
}
