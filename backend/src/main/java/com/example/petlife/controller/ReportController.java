package com.example.petlife.controller;

import com.example.petlife.service.ReportService;
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
    public String index(Model model) {
        model.addAttribute("stats", reportService.collect());
        return "reports/index";
    }
}
