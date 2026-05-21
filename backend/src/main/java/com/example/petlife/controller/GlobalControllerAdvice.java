package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.service.PlanAccessService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final PlanAccessService planAccessService;

    public GlobalControllerAdvice(PlanAccessService planAccessService) {
        this.planAccessService = planAccessService;
    }

    @ModelAttribute("currentUser")
    public LoginUser currentUser(@AuthenticationPrincipal LoginUser user) {
        return user;
    }

    @ModelAttribute("requestURI")
    public String requestURI(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("planLabel")
    public String planLabel(@AuthenticationPrincipal LoginUser user) {
        if (user == null) return null;
        return planAccessService.planLabel(user);
    }

    @ModelAttribute("canUseAiSymptom")
    public boolean canUseAiSymptom(@AuthenticationPrincipal LoginUser user) {
        return user != null && planAccessService.canUseAiSymptom(user);
    }

    @ModelAttribute("canUsePrioritySupport")
    public boolean canUsePrioritySupport(@AuthenticationPrincipal LoginUser user) {
        return user != null && planAccessService.canUsePrioritySupport(user);
    }

    @ModelAttribute("bodyClass")
    public String bodyClass(@AuthenticationPrincipal LoginUser user) {
        if (user == null) return "";
        return user.canManagePets() ? "role-admin" : "role-user";
    }
}
