package com.example.petlife.controller;

import com.example.petlife.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ForgotPasswordController {

    private final PasswordResetService passwordResetService;

    public ForgotPasswordController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // ─── Step 1: メールアドレス入力画面 ────────────────────────────────────────────

    @GetMapping("/app/forgot-password")
    public String forgotForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/app/forgot-password")
    public String forgotSubmit(@RequestParam String email) {
        passwordResetService.initiateReset(email);
        // メールアドレスの存在有無にかかわらず同じページへ（列挙攻撃防止）
        return "redirect:/app/forgot-password/sent";
    }

    @GetMapping("/app/forgot-password/sent")
    public String forgotSent() {
        return "auth/forgot-password-sent";
    }

    // ─── Step 2: 新パスワード入力画面 ─────────────────────────────────────────────

    @GetMapping("/app/reset-password")
    public String resetForm(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank() || !passwordResetService.isValidToken(token)) {
            model.addAttribute("invalid", true);
            return "auth/reset-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/app/reset-password")
    public String resetSubmit(@RequestParam String token,
                              @RequestParam String newPassword,
                              @RequestParam String confirmPassword,
                              Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("token", token);
            model.addAttribute("error", "パスワードと確認用パスワードが一致しません");
            return "auth/reset-password";
        }
        if (newPassword.length() < 8) {
            model.addAttribute("token", token);
            model.addAttribute("error", "パスワードは8文字以上で入力してください");
            return "auth/reset-password";
        }
        boolean success = passwordResetService.resetPassword(token, newPassword);
        if (!success) {
            model.addAttribute("invalid", true);
            return "auth/reset-password";
        }
        return "redirect:/app/login?reset=true";
    }
}
