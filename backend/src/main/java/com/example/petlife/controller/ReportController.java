package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.exception.ForbiddenException;
import com.example.petlife.service.ReportService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal LoginUser currentUser, Model model) {
        if (!currentUser.isAdmin()) throw new ForbiddenException("レポートは管理者のみ閲覧できます");
        model.addAttribute("stats", reportService.collect());
        return "reports/index";
    }
}
