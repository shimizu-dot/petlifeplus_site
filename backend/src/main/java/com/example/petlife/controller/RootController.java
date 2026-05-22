package com.example.petlife.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    @GetMapping("/")
    public String root() {
        return "redirect:/app/login";
    }

    @GetMapping("/i")
    public String shortI() {
        return "redirect:/app/login";
    }
}
