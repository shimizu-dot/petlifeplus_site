package com.example.petlife.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app/clinic-guide")
public class ClinicGuideController {

    @GetMapping
    public String page() {
        return "clinic/index";
    }
}
