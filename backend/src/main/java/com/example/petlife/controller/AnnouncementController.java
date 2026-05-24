package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.service.AnnouncementService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/admin/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("announcements", announcementService.findAll());
        return "admin/announcements";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam String body,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (title.isBlank() || body.isBlank()) {
            ra.addFlashAttribute("error", "タイトルと本文を入力してください");
            return "redirect:/app/admin/announcements";
        }
        announcementService.create(title, body, currentUser.id());
        ra.addFlashAttribute("success", "お知らせを登録しました");
        return "redirect:/app/admin/announcements";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        announcementService.findAll().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .ifPresent(a -> announcementService.updateIsActive(id, !a.isActive()));
        ra.addFlashAttribute("success", "ステータスを変更しました");
        return "redirect:/app/admin/announcements";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        announcementService.delete(id);
        ra.addFlashAttribute("success", "お知らせを削除しました");
        return "redirect:/app/admin/announcements";
    }
}
