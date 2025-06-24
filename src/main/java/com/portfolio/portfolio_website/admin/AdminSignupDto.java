package com.portfolio.portfolio_website.admin;

import lombok.Data;
import jakarta.validation.constraints.*;

@Data
public class AdminSignupDto {
    
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
    
    @NotBlank(message = "이름은 필수입니다.")
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;
    
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", 
             message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String password;
    
    @NotBlank(message = "비밀번호 확인은 필수입니다.")
    private String passwordConfirm;
    
    @NotBlank(message = "관리자 코드는 필수입니다.")
    private String adminCode;
    
    // 관리자 코드 검증 (간단한 보안)
    public boolean isValidAdminCode() {
        return "ADMIN2025".equals(this.adminCode); // 실제로는 더 복잡한 코드 사용
    }
}
