package com.portfolio.portfolio_website.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
@Slf4j
public class HomeController {

    /**
     * 메인 페이지
     */
    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        addSessionInfoToModel(model, session);
        return "index";
    }

    /**
     * 로그인 페이지
     */
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error, 
                       @RequestParam(required = false) String returnUrl,
                       Model model, HttpSession session) {
        if (error != null) {
            String errorMessage = switch (error) {
                case "oauth2_failed" -> "소셜 로그인에 실패했습니다. 다시 시도해주세요.";
                case "session_failed" -> "로그인 처리 중 오류가 발생했습니다.";
                case "admin_required" -> "관리자 권한이 필요합니다.";
                case "login_required" -> "로그인이 필요한 서비스입니다.";
                default -> "로그인에 실패했습니다. 다시 시도해주세요.";
            };
            model.addAttribute("error", errorMessage);
            log.warn("로그인 에러 발생: error={}, message={}", error, errorMessage);
        }
        
        if (returnUrl != null) {
            model.addAttribute("returnUrl", returnUrl);
        }
        
        addSessionInfoToModel(model, session);
        return "login";
    }

    /**
     * 테스트 페이지 (문제 진단용)
     */
    @GetMapping("/test")
    public String test(Model model, HttpSession session) {
        addSessionInfoToModel(model, session);
        return "test";
    }
    
    /**
     * 수동 로그아웃 처리 (세션 정리)
     */
    @GetMapping("/logout-manual")
    public String logoutManual(HttpSession session) {
        log.info("=== 수동 로그아웃 처리 ===");
        
        // 세션 정보 로깅
        Long userId = (Long) session.getAttribute("userId");
        String userName = (String) session.getAttribute("userName");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        String adminName = (String) session.getAttribute("adminName");
        
        log.info("로그아웃 전 세션 정보: userId={}, userName={}, isAdmin={}, adminName={}", 
                userId, userName, isAdmin, adminName);
        
        // 세션 무효화
        session.invalidate();
        log.info("✅ 세션 무효화 완료");
        
        return "redirect:/?logout=success";
    }
    
    /**
     * 세션 정보를 모델에 추가하는 공통 메서드
     */
    private void addSessionInfoToModel(Model model, HttpSession session) {
        // OAuth2 로그인 정보
        Long userId = (Long) session.getAttribute("userId");
        String userName = (String) session.getAttribute("userName");
        String userEmail = (String) session.getAttribute("userEmail");
        Boolean isOAuthLoggedIn = (Boolean) session.getAttribute("isLoggedIn");
        
        // 관리자 로그인 정보
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        String adminName = (String) session.getAttribute("adminName");
        String adminEmail = (String) session.getAttribute("adminEmail");
        
        // 로그인 상태 결정
        boolean isOAuth2Logged = (userId != null && isOAuthLoggedIn != null && isOAuthLoggedIn);
        boolean isAdminLogged = (isAdmin != null && isAdmin);
        boolean isLoggedIn = isOAuth2Logged || isAdminLogged;
        
        // 표시할 사용자 정보
        String displayName = isAdminLogged ? 
            (adminName != null ? adminName : "관리자") : 
            (userName != null ? userName : "사용자");
        
        String displayEmail = isAdminLogged ? adminEmail : userEmail;
        
        // 모델에 추가
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("isOAuth2User", isOAuth2Logged);
        model.addAttribute("isAdminUser", isAdminLogged);
        model.addAttribute("userName", displayName);
        model.addAttribute("userEmail", displayEmail);
        model.addAttribute("userId", userId);
        
        log.debug("세션 정보 추가: isLoggedIn={}, isOAuth2={}, isAdmin={}, name={}", 
                 isLoggedIn, isOAuth2Logged, isAdminLogged, displayName);
    }
}
