package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.service.line.LineUserLinkService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app/line")
public class LineLinkController {

    private final LineUserLinkService lineUserLinkService;

    public LineLinkController(LineUserLinkService lineUserLinkService) {
        this.lineUserLinkService = lineUserLinkService;
    }

    @GetMapping("/link-code")
    public String page(Model model, @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("token", lineUserLinkService.generateToken(currentUser.id()));
        return "line/link-code";
    }

    @PostMapping("/link-code/refresh")
    public String refresh() {
        return "redirect:/app/line/link-code";
    }
}
