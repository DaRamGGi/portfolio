package com.portfolio.portfolio_website.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Arrays;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    
    private final AdminRepository adminRepository;
    private final AdminService adminService;
    
    // 관리자 회원가입 폼
    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("adminSignupDto", new AdminSignupDto());
        return "admin-signup";
    }
    
    // 관리자 회원가입 처리
    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute AdminSignupDto adminSignupDto, 
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        
        // 유효성 검사 오류가 있는 경우
        if (bindingResult.hasErrors()) {
            return "admin-signup";
        }
        
        // 관리자 코드 검증
        if (!adminSignupDto.isValidAdminCode()) {
            model.addAttribute("errorMessage", "관리자 코드가 올바르지 않습니다.");
            return "admin-signup";
        }
        
        // 이메일 중복 체크
        if (adminRepository.existsByEmail(adminSignupDto.getEmail())) {
            model.addAttribute("emailError", "이미 사용 중인 이메일입니다.");
            return "admin-signup";
        }
        
        // 비밀번호 확인 체크
        if (!adminSignupDto.getPassword().equals(adminSignupDto.getPasswordConfirm())) {
            model.addAttribute("passwordError", "비밀번호가 일치하지 않습니다.");
            return "admin-signup";
        }
        
        try {
            // 관리자 생성
            adminService.createAdmin(
                adminSignupDto.getEmail(),
                adminSignupDto.getName(),
                adminSignupDto.getPassword()
            );
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "관리자 계정이 성공적으로 생성되었습니다. 로그인해주세요.");
            
            return "redirect:/login";
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "회원가입 중 오류가 발생했습니다. 다시 시도해주세요.");
            return "admin-signup";
        }
    }
    
    // 관리자 로그인 처리 (POST)
    @PostMapping("/login")
    public String login(@RequestParam String email, 
                       @RequestParam String password,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        
        AdminEntity admin = adminService.authenticate(email, password);
        
        if (admin != null) {
            // 세션에 관리자 정보 저장
            session.setAttribute("adminId", admin.getId());
            session.setAttribute("adminEmail", admin.getEmail());
            session.setAttribute("adminName", admin.getName());
            session.setAttribute("isAdmin", true);
            
            System.out.println("=== 관리자 로그인 성공 ===");
            System.out.println("관리자: " + admin.getName() + " (" + admin.getEmail() + ")");
            
            return "redirect:/admin/dashboard";
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "이메일 또는 비밀번호가 올바르지 않습니다.");
            return "redirect:/login?error=admin_login_failed";
        }
    }
    
    // 이메일 중복 체크 (AJAX)
    @PostMapping("/check-email")
    @ResponseBody
    public boolean checkEmail(@RequestParam String email) {
        return !adminRepository.existsByEmail(email);
    }
    
    // 관리자 대시보드
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // 세션에서 관리자 정보 확인
        Long adminId = (Long) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/login";
        }
        
        AdminEntity admin = adminRepository.findById(adminId).orElse(null);
        if (admin != null) {
            model.addAttribute("admin", admin);
            model.addAttribute("totalAdmins", adminRepository.count());
        }
        
        return "admin/dashboard";
    }
    
    // 관리자 목록 (관리자만 접근)
    @GetMapping("/list")
    public String adminList(HttpSession session, Model model) {
        // 관리자 권한 확인
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login";
        }
        
        List<AdminEntity> admins = adminRepository.findAllByOrderByCreatedAtDesc();
        model.addAttribute("admins", admins);
        return "admin/admin-list";
    }
    
    // 관리자 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // 세션 무효화
        session.invalidate();
        return "redirect:/login?logout=success";
    }
}
