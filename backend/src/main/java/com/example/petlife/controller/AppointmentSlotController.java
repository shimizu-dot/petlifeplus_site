package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.entity.AppointmentSlotEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.mapper.AppointmentSlotMapper;
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

    private final AppointmentSlotMapper slotMapper;

    public AppointmentSlotController(AppointmentSlotMapper slotMapper) {
        this.slotMapper = slotMapper;
    }

    @GetMapping
    public String page(Model model, @AuthenticationPrincipal LoginUser currentUser) {
        ensureAdmin(currentUser);
        model.addAttribute("slots", slotMapper.findAll());
        return "appointments/slot-management";
    }

    @PostMapping
    public String create(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slotDatetime,
                         @RequestParam(required = false) String note,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        ensureAdmin(currentUser);
        if (slotDatetime == null || !slotDatetime.isAfter(LocalDateTime.now())) {
            ra.addFlashAttribute("error", "予約枠は未来日時で指定してください");
            return "redirect:/app/admin/appointment-slots";
        }
        AppointmentSlotEntity row = new AppointmentSlotEntity(null, slotDatetime, note, currentUser.id(), null, null);
        slotMapper.insert(row);
        ra.addFlashAttribute("success", "予約枠を追加しました");
        return "redirect:/app/admin/appointment-slots";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        ensureAdmin(currentUser);
        if (slotMapper.countBookings(id) > 0) {
            ra.addFlashAttribute("error", "申請済みの予約枠は削除できません");
            return "redirect:/app/admin/appointment-slots";
        }
        slotMapper.softDelete(id, LocalDateTime.now());
        ra.addFlashAttribute("success", "予約枠を削除しました");
        return "redirect:/app/admin/appointment-slots";
    }

    private void ensureAdmin(LoginUser currentUser) {
        if (!currentUser.isAdmin()) throw new BadRequestException("管理者のみアクセスできます");
    }
}
