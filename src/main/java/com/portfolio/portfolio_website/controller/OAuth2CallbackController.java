package com.portfolio.portfolio_website.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

@RestController
@Slf4j
public class OAuth2CallbackController {

    /**
     * 카카오 OAuth2 콜백 디버깅용
     */
    @GetMapping("/login/oauth2/code/kakao")
    public String kakaoCallback(
            HttpServletRequest request,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        
        log.info("=== 카카오 OAuth2 콜백 수신 ===");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Query String: {}", request.getQueryString());
        log.info("Code: {}", code);
        log.info("State: {}", state);
        log.info("Error: {}", error);
        
        // 모든 파라미터 로깅
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = request.getParameter(paramName);
            log.info("Parameter: {} = {}", paramName, paramValue);
        }
        
        return "OAuth2 콜백 수신됨 - 로그 확인하세요";
    }
}
