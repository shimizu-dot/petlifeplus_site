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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/app/admin/appointment-slots")
public class AppointmentSlotController {

    private final AppointmentSlotService slotService;

    public AppointmentSlotController(AppointmentSlotService slotService) {
        this.slotService = slotService;
    }

    @GetMapping
    public String page(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        LocalDate targetDate = (date != null) ? date : LocalDate.now().plusDays(1);

        model.addAttribute("bodyClass", "role-admin");
        model.addAttribute("targetDate", targetDate);

        if (targetDate.isBefore(LocalDate.now())) {
            model.addAttribute("error", "過去の日付は指定できません。本日以降の日付を選択してください。");
            return "appointments/slot-management";
        }

        model.addAttribute("daySlots", slotService.getDaySlotsWithStatus(targetDate));
        return "appointments/slot-management";
    }

    @PostMapping
    public String create(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slotDatetime,
                         @RequestParam(required = false) String note,
                         @RequestParam(defaultValue = "false") boolean isBlocked,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        LocalDate targetDate = slotDatetime.toLocalDate();
        try {
            slotService.create(slotDatetime, note, isBlocked, currentUser);
            ra.addFlashAttribute("success", isBlocked ? "ブロック枠を登録しました" : "追加枠を登録しました");
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/app/admin/appointment-slots?date=" + targetDate;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        try {
            slotService.delete(id, currentUser);
            ra.addFlashAttribute("success", "削除しました");
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        String redirect = "/app/admin/appointment-slots";
        if (date != null) redirect += "?date=" + date;
        return "redirect:" + redirect;
    }
}
