package com.portfolio.portfolio_website.controller;

import com.portfolio.portfolio_website.user.UserEntity;
import com.portfolio.portfolio_website.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DebugController {
    
    private final UserRepository userRepository;
    
    /**
     * OAuth2 인증 상태 확인
     */
    @GetMapping("/debug/oauth2")
    @ResponseBody
    public Map<String, Object> debugOAuth2(Authentication authentication, HttpSession session) {
        Map<String, Object> debug = new HashMap<>();
        
        log.info("=== DEBUG OAuth2 상태 확인 ===");
        
        // Authentication 정보
        debug.put("authenticationExists", authentication != null);
        if (authentication != null) {
            debug.put("authenticationType", authentication.getClass().getSimpleName());
            debug.put("isAuthenticated", authentication.isAuthenticated());
            debug.put("principal", authentication.getPrincipal().getClass().getSimpleName());
            
            if (authentication instanceof OAuth2AuthenticationToken) {
                OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                OAuth2User oauth2User = oauth2Token.getPrincipal();
                
                debug.put("registrationId", oauth2Token.getAuthorizedClientRegistrationId());
                debug.put("attributes", oauth2User.getAttributes());
                
                // 카카오 정보 추출
                if ("kakao".equals(oauth2Token.getAuthorizedClientRegistrationId())) {
                    Long kakaoId = (Long) oauth2User.getAttributes().get("id");
                    debug.put("kakaoId", kakaoId);
                    
                    // DB에서 해당 사용자 찾기
                    if (kakaoId != null) {
                        UserEntity user = userRepository.findByProviderAndProviderId("kakao", String.valueOf(kakaoId))
                                .orElse(null);
                        debug.put("userFoundInDB", user != null);
                        if (user != null) {
                            debug.put("dbUser", Map.of(
                                "id", user.getId(),
                                "name", user.getName(),
                                "email", user.getEmail(),
                                "provider", user.getProvider(),
                                "providerId", user.getProviderId()
                            ));
                        }
                    }
                }
            }
        }
        
        // 세션 정보
        debug.put("sessionId", session.getId());
        debug.put("sessionAttributes", Map.of(
            "userId", session.getAttribute("userId"),
            "userName", session.getAttribute("userName"),
            "userEmail", session.getAttribute("userEmail"),
            "isLoggedIn", session.getAttribute("isLoggedIn")
        ));
        
        // DB 상태
        List<UserEntity> allUsers = userRepository.findAll();
        debug.put("totalUsersInDB", allUsers.size());
        debug.put("allUsers", allUsers.stream().map(u -> Map.of(
            "id", u.getId(),
            "name", u.getName(),
            "email", u.getEmail(),
            "provider", u.getProvider(),
            "providerId", u.getProviderId()
        )).toList());
        
        log.info("DEBUG 결과: {}", debug);
        return debug;
    }
    
    /**
     * 수동으로 세션에 테스트 데이터 저장
     */
    @GetMapping("/debug/set-session")
    @ResponseBody
    public String setTestSession(HttpSession session) {
        session.setAttribute("userId", 999L);
        session.setAttribute("userName", "테스트사용자");
        session.setAttribute("userEmail", "test@test.com");
        session.setAttribute("isLoggedIn", true);
        
        log.info("테스트 세션 데이터 저장 완료");
        return "테스트 세션 데이터 저장 완료";
    }
}
