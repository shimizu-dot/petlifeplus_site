package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.common.PageResponse;
import com.example.petlife.dto.subscription.SubscriptionRow;
import com.example.petlife.mapper.SubscriptionMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/app/subscriptions")
public class SubscriptionController {

    private final SubscriptionMapper subscriptionMapper;

    public SubscriptionController(SubscriptionMapper subscriptionMapper) {
        this.subscriptionMapper = subscriptionMapper;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        int offset = (page - 1) * size;
        List<SubscriptionRow> items;
        long total;
        if (currentUser.isAdmin()) {
            items = subscriptionMapper.findAllRows(size, offset);
            total = subscriptionMapper.countAll();
        } else {
            items = subscriptionMapper.findRowsByUserId(currentUser.id(), size, offset);
            total = subscriptionMapper.countByUserId(currentUser.id());
        }
        model.addAttribute("page", new PageResponse<>(items, page, size, total));
        model.addAttribute("isAdminView", currentUser.isAdmin());
        return "subscriptions/index";
    }
}
