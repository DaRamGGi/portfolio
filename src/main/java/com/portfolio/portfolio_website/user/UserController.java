package com.portfolio.portfolio_website.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserRepository userRepository;

    /**
     * 세션 정보 확인 (디버깅용)
     */
    @GetMapping("/api/session/info")
    @ResponseBody
    public Map<String, Object> getSessionInfo(HttpSession session) {
        Map<String, Object> sessionInfo = new HashMap<>();
        
        // OAuth2 로그인 정보
        sessionInfo.put("userId", session.getAttribute("userId"));
        sessionInfo.put("userName", session.getAttribute("userName"));
        sessionInfo.put("userEmail", session.getAttribute("userEmail"));
        sessionInfo.put("userProvider", session.getAttribute("userProvider"));
        sessionInfo.put("isLoggedIn", session.getAttribute("isLoggedIn"));
        
        // 관리자 로그인 정보
        sessionInfo.put("isAdmin", session.getAttribute("isAdmin"));
        sessionInfo.put("adminId", session.getAttribute("adminId"));
        sessionInfo.put("adminName", session.getAttribute("adminName"));
        sessionInfo.put("adminEmail", session.getAttribute("adminEmail"));
        
        // 세션 ID
        sessionInfo.put("sessionId", session.getId());
        
        log.info("세션 정보 조회: {}", sessionInfo);
        return sessionInfo;
    }

    /**
     * 사용자 정보 확인 (JSON 응답) - 디버깅용
     */
    @GetMapping("/api/user/info")
    @ResponseBody
    public Map<String, Object> getUserInfo(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            
            log.info("현재 로그인한 사용자 정보: {}", oauth2User.getAttributes());
            return oauth2User.getAttributes();
        }
        return Map.of("message", "로그인하지 않은 사용자");
    }

    /**
     * 프로필 페이지
     */
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauth2Token.getPrincipal();
            
            // OAuth2 사용자 정보
            Map<String, Object> attributes = oauth2User.getAttributes();
            model.addAttribute("oauth2User", attributes);
            
            // DB에서 사용자 정보 조회
            if ("kakao".equals(oauth2Token.getAuthorizedClientRegistrationId())) {
                Long kakaoId = (Long) attributes.get("id");
                userRepository.findByProviderAndProviderId("kakao", String.valueOf(kakaoId))
                    .ifPresent(user -> model.addAttribute("user", user));
            }
        }
        
        return "profile";
    }
}
