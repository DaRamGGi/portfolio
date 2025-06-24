package com.portfolio.portfolio_website.config;

import com.portfolio.portfolio_website.user.UserEntity;
import com.portfolio.portfolio_website.user.UserRepository;
import com.portfolio.portfolio_website.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final UserService userService;
    private final UserRepository userRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authz -> authz
                // 공개 페이지들
                .requestMatchers("/", "/login", "/admin/signup", "/admin/check-email").permitAll()
                .requestMatchers("/login/oauth2/code/**").permitAll()  // OAuth2 콜백
                .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**").permitAll()  // 정적 리소스
                
                // 관리자 전용 페이지들 (임시로 주석 처리)
                // .requestMatchers("/admin/**", "/project/payment/admin/**").hasRole("ADMIN")
                
                // 나머지는 모두 허용 (임시)
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(userService)
                )
                .successHandler((request, response, authentication) -> {
                    log.info("=== OAuth2 로그인 성공 핸들러 실행 ===");
                    
                    try {
                        if (authentication instanceof OAuth2AuthenticationToken) {
                            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
                            OAuth2User oauth2User = oauth2Token.getPrincipal();
                            String provider = oauth2Token.getAuthorizedClientRegistrationId();
                            
                            log.info("OAuth2 인증 정보:");
                            log.info("  - provider: {}", provider);
                            log.info("  - attributes: {}", oauth2User.getAttributes());
                            
                            // 세션에 사용자 정보 저장
                            HttpSession session = request.getSession();
                            
                            if ("kakao".equals(provider)) {
                                Map<String, Object> attributes = oauth2User.getAttributes();
                                Long kakaoId = (Long) attributes.get("id");
                                
                                log.info("카카오 ID: {}", kakaoId);
                                
                                // DB에서 사용자 정보 조회
                                UserEntity user = userRepository.findByProviderAndProviderId("kakao", String.valueOf(kakaoId))
                                    .orElse(null);
                                
                                if (user != null) {
                                    log.info("✅ DB에서 사용자 찾음:");
                                    log.info("  - DB user.id: {}", user.getId());
                                    log.info("  - DB user.name: {}", user.getName());
                                    log.info("  - DB user.email: {}", user.getEmail());
                                    log.info("  - DB user.provider: {}", user.getProvider());
                                    log.info("  - DB user.providerId: {}", user.getProviderId());
                                    
                                    // 세션에 저장
                                    session.setAttribute("userId", user.getId());
                                    session.setAttribute("userName", user.getName());
                                    session.setAttribute("userEmail", user.getEmail());
                                    session.setAttribute("userProvider", user.getProvider());
                                    session.setAttribute("isLoggedIn", true);
                                    
                                    log.info("✅ 세션에 사용자 정보 저장 완료:");
                                    log.info("  - session.userId: {}", session.getAttribute("userId"));
                                    log.info("  - session.userName: {}", session.getAttribute("userName"));
                                    log.info("  - session.userEmail: {}", session.getAttribute("userEmail"));
                                    log.info("  - session.isLoggedIn: {}", session.getAttribute("isLoggedIn"));
                                    
                                    // 마지막 로그인 시간 업데이트
                                    user.updateLastLogin();
                                    userRepository.save(user);
                                    
                                } else {
                                    log.error("❌ DB에서 사용자 정보를 찾을 수 없습니다!");
                                    log.error("  - 찾으려고 한 정보: provider=kakao, providerId={}", kakaoId);
                                    
                                    // DB에 저장된 모든 카카오 사용자 확인
                                    java.util.List<UserEntity> allKakaoUsers = userRepository.findByProvider("kakao");
                                    log.error("  - DB에 저장된 카카오 사용자 수: {}", allKakaoUsers.size());
                                    for (UserEntity kakaoUser : allKakaoUsers) {
                                        log.error("    * id={}, providerId={}, name={}, email={}", 
                                                kakaoUser.getId(), kakaoUser.getProviderId(), 
                                                kakaoUser.getName(), kakaoUser.getEmail());
                                    }
                                }
                            }
                        } else {
                            log.error("❌ OAuth2AuthenticationToken이 아닙니다: {}", authentication.getClass().getSimpleName());
                        }
                        
                        log.info("🔄 메인 페이지로 리다이렉트");
                        response.sendRedirect("/");
                        
                    } catch (Exception e) {
                        log.error("❌ OAuth2 성공 핸들러 처리 중 오류: {}", e.getMessage(), e);
                        response.sendRedirect("/login?error=session_failed");
                    }
                })
                .failureHandler((request, response, exception) -> {
                    log.error("=== OAuth2 로그인 실패 ===");
                    log.error("실패 원인: {}", exception.getMessage(), exception);
                    response.sendRedirect("/login?error=oauth2_failed");
                })
                .defaultSuccessUrl("/", true)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    log.info("=== 로그아웃 처리 ===");
                    
                    // 세션 무효화 및 정리
                    HttpSession session = request.getSession(false);
                    if (session != null) {
                        log.info("세션 정리 전 - userId: {}, userName: {}", 
                                session.getAttribute("userId"), session.getAttribute("userName"));
                        
                        session.invalidate();
                        log.info("✅ 세션 무효화 완료");
                    }
                    
                    response.sendRedirect("/");
                })
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
