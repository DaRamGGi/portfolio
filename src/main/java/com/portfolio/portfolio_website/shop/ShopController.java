package com.portfolio.portfolio_website.shop;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/project/payment")
@RequiredArgsConstructor
@Slf4j
public class ShopController {
    
    private final ShopRepository shopRepository;
    
    // 상품 목록 페이지
    @GetMapping
    public String shopList(Model model, HttpSession session,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "9") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ShopEntity> products = shopRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        // 할인 상품들
        List<ShopEntity> discountedProducts = shopRepository.findDiscountedProducts();
        
        model.addAttribute("products", products);
        model.addAttribute("discountedProducts", discountedProducts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        
        // 세션 정보 추가
        addSessionInfoToModel(model, session);
        
        return "shop/shop-list";
    }
    
    // 상품 상세 페이지
    @GetMapping("/{sNo}")
    public String shopDetail(@PathVariable Long sNo, Model model, HttpSession session) {
        ShopEntity product = shopRepository.findById(sNo)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        
        model.addAttribute("product", product);
        
        // 세션 정보 추가
        addSessionInfoToModel(model, session);
        
        return "shop/shop-detail";
    }
    
    // 상품 검색
    @GetMapping("/search")
    public String searchProducts(@RequestParam String keyword, Model model, HttpSession session) {
        List<ShopEntity> searchResults = shopRepository.findBysTitleContainingIgnoreCase(keyword);
        
        model.addAttribute("products", searchResults);
        model.addAttribute("keyword", keyword);
        
        // 세션 정보 추가
        addSessionInfoToModel(model, session);
        
        return "shop/shop-search";
    }
    
    // 관리자: 상품 등록 폼
    @GetMapping("/admin/add")
    public String addProductForm(Model model, HttpSession session) {
        // 관리자 권한 확인
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login?error=admin_required";
        }
        
        model.addAttribute("product", new ShopEntity());
        return "shop/shop-add";
    }
    
    // 관리자: 상품 등록 처리
    @PostMapping("/admin/add")
    public String addProduct(@ModelAttribute ShopEntity product, HttpSession session) {
        // 관리자 권한 확인
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login?error=admin_required";
        }
        
        shopRepository.save(product);
        return "redirect:/project/payment";
    }
    
    // 관리자: 상품 수정 폼
    @GetMapping("/admin/edit/{sNo}")
    public String editProductForm(@PathVariable Long sNo, Model model, HttpSession session) {
        // 관리자 권한 확인
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login?error=admin_required";
        }
        
        ShopEntity product = shopRepository.findById(sNo)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        
        model.addAttribute("product", product);
        return "shop/shop-edit";
    }
    
    // 관리자: 상품 수정 처리
    @PostMapping("/admin/edit/{sNo}")
    public String editProduct(@PathVariable Long sNo, @ModelAttribute ShopEntity product, HttpSession session) {
        // 관리자 권한 확인
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login?error=admin_required";
        }
        
        product.setSNo(sNo);
        shopRepository.save(product);
        return "redirect:/project/payment/" + sNo;
    }
    
    // 관리자: 상품 삭제
    @PostMapping("/admin/delete/{sNo}")
    public String deleteProduct(@PathVariable Long sNo, HttpSession session) {
        // 관리자 권한 확인
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            return "redirect:/login?error=admin_required";
        }
        
        shopRepository.deleteById(sNo);
        return "redirect:/project/payment";
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
        
        log.debug("ShopController 세션 정보 추가: isLoggedIn={}, isOAuth2={}, isAdmin={}, name={}", 
                 isLoggedIn, isOAuth2Logged, isAdminLogged, displayName);
    }
}
