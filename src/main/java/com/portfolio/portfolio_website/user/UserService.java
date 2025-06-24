package com.portfolio.portfolio_website.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("=== OAuth2 사용자 정보 로드 시작 ===");
        log.info("Registration ID: {}", userRequest.getClientRegistration().getRegistrationId());
        
        try {
            OAuth2User oauth2User = super.loadUser(userRequest);
            log.info("OAuth2User 로드 성공: {}", oauth2User.getAttributes());
            
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            Map<String, Object> attributes = oauth2User.getAttributes();
            
            // 카카오 로그인 처리
            if ("kakao".equals(registrationId)) {
                processKakaoUser(attributes);
            }
            
            log.info("=== OAuth2 사용자 정보 처리 완료 ===");
            return oauth2User;
            
        } catch (Exception e) {
            log.error("=== OAuth2 사용자 정보 로드 실패 ===");
            log.error("에러 메시지: {}", e.getMessage());
            log.error("에러 타입: {}", e.getClass().getSimpleName());
            log.error("스택 트레이스:", e);
            
            // MariaDB 연결 상태 확인
            try {
                long userCount = userRepository.count();
                log.info("MariaDB 연결 상태 확인 - 총 사용자 수: {}", userCount);
            } catch (Exception dbError) {
                log.error("MariaDB 연결 실패: {}", dbError.getMessage());
            }
            
            throw new OAuth2AuthenticationException("사용자 정보 로드 실패: " + e.getMessage());
        }
    }

    private void processKakaoUser(Map<String, Object> attributes) {
        try {
            // 카카오 사용자 정보 추출
            Long kakaoId = (Long) attributes.get("id");
            
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
            
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            String nickname = properties != null ? (String) properties.get("nickname") : "카카오 사용자";
            String profileImage = properties != null ? (String) properties.get("profile_image") : null;
            
            log.info("🔍 카카오 사용자 정보 추출:");
            log.info("  - kakaoId: {}", kakaoId);
            log.info("  - email: {}", email);
            log.info("  - nickname: {}", nickname);
            log.info("  - profileImage: {}", profileImage);
            
            // 기존 사용자 확인 (providerId로 먼저 찾기)
            Optional<UserEntity> existingUser = userRepository.findByProviderAndProviderId("kakao", String.valueOf(kakaoId));
            
            UserEntity user;
            if (existingUser.isPresent()) {
                // 기존 사용자 정보 업데이트
                user = updateExistingUser(existingUser.get(), email, nickname, profileImage);
                log.info("✅ 기존 사용자 업데이트 완료");
            } else {
                // 새 사용자 생성
                user = createNewUser(kakaoId, email, nickname, profileImage);
                log.info("✅ 새 사용자 생성 완료");
            }
            
            // UserService에서도 세션에 저장 시도
            try {
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                HttpSession session = attr.getRequest().getSession();
                
                session.setAttribute("userId", user.getId());
                session.setAttribute("userName", user.getName());
                session.setAttribute("userEmail", user.getEmail());
                session.setAttribute("userProvider", "kakao");
                session.setAttribute("isLoggedIn", true);
                
                log.info("🎯 UserService에서 세션 저장 완료:");
                log.info("  - userId: {}", user.getId());
                log.info("  - userName: {}", user.getName());
                log.info("  - userEmail: {}", user.getEmail());
                
            } catch (Exception sessionError) {
                log.warn("⚠️ UserService에서 세션 저장 실패: {}", sessionError.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ 카카오 사용자 처리 중 오류 발생", e);
            throw new OAuth2AuthenticationException("카카오 사용자 정보 처리 실패");
        }
    }

    private UserEntity updateExistingUser(UserEntity user, String email, String nickname, String profileImage) {
        // 기존 사용자 정보 업데이트
        user.setEmail(email);
        user.setName(nickname);
        user.setProfileImage(profileImage);
        user.updateLastLogin();
        
        UserEntity savedUser = userRepository.save(user);
        log.info("📝 기존 사용자 정보 업데이트: id={}, email={}, name={}", 
                savedUser.getId(), savedUser.getEmail(), savedUser.getName());
        return savedUser;
    }

    private UserEntity createNewUser(Long kakaoId, String email, String nickname, String profileImage) {
        UserEntity newUser = UserEntity.builder()
                .email(email)
                .name(nickname)
                .profileImage(profileImage)
                .provider("kakao")
                .providerId(String.valueOf(kakaoId))
                .build();
        
        UserEntity savedUser = userRepository.save(newUser);
        log.info("➕ 새 사용자 생성: id={}, email={}, name={}, providerId={}", 
                savedUser.getId(), savedUser.getEmail(), savedUser.getName(), savedUser.getProviderId());
        return savedUser;
    }
}
