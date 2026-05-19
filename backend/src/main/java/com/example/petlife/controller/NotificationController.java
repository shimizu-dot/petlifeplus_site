package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.mapper.NotificationMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/notifications")
public class NotificationController {

    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        int offset = (page - 1) * size;
        model.addAttribute("notifications",
                notificationMapper.findByUserId(currentUser.id(), size, offset));
        model.addAttribute("total", notificationMapper.countByUserId(currentUser.id()));
        model.addAttribute("unreadCount", notificationMapper.countUnreadByUserId(currentUser.id()));
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        return "notifications/index";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(@PathVariable Long id,
                             @AuthenticationPrincipal LoginUser currentUser,
                             RedirectAttributes ra) {
        notificationMapper.markAsRead(id, currentUser.id());
        return "redirect:/app/notifications";
    }

    @PostMapping("/read-all")
    public String markAllAsRead(@AuthenticationPrincipal LoginUser currentUser,
                                RedirectAttributes ra) {
        notificationMapper.markAllAsRead(currentUser.id());
        ra.addFlashAttribute("success", "すべての通知を既読にしました");
        return "redirect:/app/notifications";
    }
}
