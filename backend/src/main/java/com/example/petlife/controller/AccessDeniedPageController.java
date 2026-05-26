package com.example.petlife.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccessDeniedPageController {

    @GetMapping("/app/access-denied")
    public String accessDenied() {
        return "error/access-denied";
    }
}
