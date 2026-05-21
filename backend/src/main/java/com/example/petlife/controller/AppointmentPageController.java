package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.appointment.GeneralAppointmentForm;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.service.AppointmentService;
import com.example.petlife.service.PetService;
import com.example.petlife.service.PlanAccessService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/app/appointments")
public class AppointmentPageController {

    private final AppointmentService appointmentService;
    private final PetService petService;
    private final PlanAccessService planAccessService;

    public AppointmentPageController(AppointmentService appointmentService,
                                     PetService petService,
                                     PlanAccessService planAccessService) {
        this.appointmentService = appointmentService;
        this.petService = petService;
        this.planAccessService = planAccessService;
    }

    @GetMapping
    public String page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        ensureAccessible(currentUser);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new GeneralAppointmentForm());
        }
        LocalDate slotDate = (date != null) ? date : LocalDate.now();
        model.addAttribute("isAdminView", currentUser.isAdmin());
        model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                .filter(p -> p.deceasedAt() == null)
                .toList());
        model.addAttribute("selectedDate", slotDate);
        model.addAttribute("availableSlots", appointmentService.generateAvailableSlots(slotDate));
        model.addAttribute("page", appointmentService.listForApp(page, size, currentUser));
        return "appointments/index";
    }

    @PostMapping
    public String create(@Valid GeneralAppointmentForm form,
                         BindingResult result,
                         @AuthenticationPrincipal LoginUser currentUser,
                         Model model,
                         RedirectAttributes ra) {
        ensureAccessible(currentUser);
        if (currentUser.isAdmin()) {
            throw new BadRequestException("管理者はこの画面から予約作成できません");
        }
        if (result.hasErrors()) {
            LocalDate slotDate = form.getScheduledAt() != null ? form.getScheduledAt().toLocalDate() : LocalDate.now();
            model.addAttribute("isAdminView", false);
            model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                    .filter(p -> p.deceasedAt() == null)
                    .toList());
            model.addAttribute("selectedDate", slotDate);
            model.addAttribute("availableSlots", appointmentService.generateAvailableSlots(slotDate));
            model.addAttribute("page", appointmentService.listForApp(1, 10, currentUser));
            return "appointments/index";
        }
        appointmentService.createGeneralCare(form.getPetId(), form.getScheduledAt(), form.getNote(), currentUser);
        ra.addFlashAttribute("success", "診療予約を登録しました");
        return "redirect:/app/appointments";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal LoginUser currentUser,
                          RedirectAttributes ra) {
        if (!currentUser.isAdmin()) throw new BadRequestException("管理者のみ承認できます");
        appointmentService.approve(id);
        ra.addFlashAttribute("success", "予約を承認しました");
        return "redirect:/app/appointments";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (!currentUser.isAdmin()) throw new BadRequestException("管理者のみ却下できます");
        appointmentService.reject(id);
        ra.addFlashAttribute("success", "予約を却下しました");
        return "redirect:/app/appointments";
    }

    private void ensureAccessible(LoginUser currentUser) {
        if (!currentUser.isAdmin() && !planAccessService.canUseAiSymptom(currentUser)) {
            throw new BadRequestException("この機能はスタンダード以上で利用できます");
        }
    }
}
