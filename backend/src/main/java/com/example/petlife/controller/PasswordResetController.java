package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/password-resets")
public class PasswordResetController {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordResetController(UserMapper userMapper, BCryptPasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String form() {
        return "auth/password-change";
    }

    @PostMapping
    public String change(@RequestParam String currentPassword,
                         @RequestParam String newPassword,
                         @RequestParam String confirmPassword,
                         @AuthenticationPrincipal LoginUser currentUser,
                         Model model,
                         RedirectAttributes ra) {
        if (!passwordEncoder.matches(currentPassword, currentUser.passwordHash())) {
            model.addAttribute("error", "現在のパスワードが正しくありません");
            return "auth/password-change";
        }
        if (newPassword.length() < 8) {
            model.addAttribute("error", "新しいパスワードは8文字以上で入力してください");
            return "auth/password-change";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "新しいパスワードと確認用パスワードが一致しません");
            return "auth/password-change";
        }
        userMapper.updatePasswordById(currentUser.id(), passwordEncoder.encode(newPassword));
        ra.addFlashAttribute("success", "パスワードを変更しました");
        return "redirect:/app/dashboard";
    }
}
