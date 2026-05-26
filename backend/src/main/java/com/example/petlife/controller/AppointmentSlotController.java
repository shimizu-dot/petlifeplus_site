package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.service.AppointmentSlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/app/admin/appointment-slots")
public class AppointmentSlotController {

    private final AppointmentSlotService slotService;

    public AppointmentSlotController(AppointmentSlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    public String page(Model model, @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("slots", slotService.list(currentUser));
        return "appointments/slot-management";
    }

    @PostMapping
    public String create(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slotDatetime,
                         @RequestParam(required = false) String note,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        try {
            slotService.create(slotDatetime, note, currentUser);
            ra.addFlashAttribute("success", "予約枠を追加しました");
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/app/admin/appointment-slots";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        try {
            slotService.delete(id, currentUser);
            ra.addFlashAttribute("success", "予約枠を削除しました");
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/app/admin/appointment-slots";
    }
}
