package com.example.petlife.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class AuthController {

    private final boolean loginMaintenanceMode;

    public AuthController(@Value("${app.login-maintenance-mode:false}") boolean loginMaintenanceMode) {
        this.loginMaintenanceMode = loginMaintenanceMode;
    }

    @GetMapping("/login")
    public String loginPage() {
        if (loginMaintenanceMode) {
            return "redirect:/webapp.html?maintenance=1";
        }
        return "auth/login";
    }
}
