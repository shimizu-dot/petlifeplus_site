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
import java.util.List;

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
                       @RequestParam(defaultValue = "scheduledAt") String sortBy,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        ensureAccessible(currentUser);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new GeneralAppointmentForm());
        }
        var businessHours = appointmentService.getBusinessHours();
        LocalDate slotDate = (date != null) ? date : LocalDate.now();
        model.addAttribute("isAdminView", currentUser.canOperateAppointments());
        model.addAttribute("canChooseOnline",
                currentUser.canOperateAppointments() ||
                planAccessService.resolvePlanTier(currentUser) == PlanAccessService.PlanTier.PREMIUM);
        model.addAttribute("businessHours", businessHours);
        model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                .filter(p -> p.deceasedAt() == null)
                .toList());
        model.addAttribute("selectedDate", slotDate);
        model.addAttribute("availableSlots", appointmentService.generateAvailableSlots(slotDate));
        model.addAttribute("sortBy", "pet".equalsIgnoreCase(sortBy) ? "pet" : "scheduledAt");
        model.addAttribute("page", appointmentService.listForApp(page, size, currentUser, sortBy));
        return "appointments/index";
    }

    @PostMapping
    public String create(@Valid GeneralAppointmentForm form,
                         BindingResult result,
                         @AuthenticationPrincipal LoginUser currentUser,
                         Model model,
                         RedirectAttributes ra) {
        ensureAccessible(currentUser);
        if (result.hasErrors()) {
            populatePageModel(form, currentUser, model);
            return "appointments/index";
        }
        try {
            appointmentService.createGeneralCare(form.getPetId(), form.getScheduledAt(), form.getNote(), form.getChannel(), currentUser);
        } catch (BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            populatePageModel(form, currentUser, model);
            return "appointments/index";
        }
        ra.addFlashAttribute("success", "診療予約を登録しました");
        return "redirect:/app/appointments";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal LoginUser currentUser,
                          RedirectAttributes ra) {
        if (!currentUser.canOperateAppointments()) throw new BadRequestException("SUPER・獣医師・スタッフのみ承認できます");
        appointmentService.approve(id, currentUser.id());
        ra.addFlashAttribute("success", "予約を承認しました");
        return "redirect:/app/appointments";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (!currentUser.canOperateAppointments()) throw new BadRequestException("SUPER・獣医師・スタッフのみ却下できます");
        appointmentService.reject(id, currentUser.id());
        ra.addFlashAttribute("success", "予約を却下しました");
        return "redirect:/app/appointments";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (currentUser.hasStaffAccess()) {
            throw new BadRequestException("この操作はペットオーナーのみ実行できます");
        }
        appointmentService.cancelRequestedByOwner(id, currentUser);
        ra.addFlashAttribute("success", "予約をキャンセルしました");
        return "redirect:/app/appointments";
    }

    @PostMapping("/delete-selected")
    public String deleteSelected(@RequestParam(required = false) List<Long> appointmentIds,
                                 @AuthenticationPrincipal LoginUser currentUser,
                                 RedirectAttributes ra) {
        ensureAccessible(currentUser);
        int deletedCount = appointmentService.deleteSelected(appointmentIds, currentUser);
        ra.addFlashAttribute("success", deletedCount + "件の予約を削除しました");
        return "redirect:/app/appointments";
    }

    private void ensureAccessible(LoginUser currentUser) {
        if (!currentUser.canOperateAppointments() && !planAccessService.canUseAppointments(currentUser)) {
            throw new BadRequestException("この機能はスタンダード以上で利用できます");
        }
    }

    private void populatePageModel(GeneralAppointmentForm form, LoginUser currentUser, Model model) {
        LocalDate slotDate = form.getScheduledAt() != null ? form.getScheduledAt().toLocalDate() : LocalDate.now();
        model.addAttribute("isAdminView", currentUser.canOperateAppointments());
        model.addAttribute("canChooseOnline",
                currentUser.canOperateAppointments() ||
                planAccessService.resolvePlanTier(currentUser) == PlanAccessService.PlanTier.PREMIUM);
        model.addAttribute("businessHours", appointmentService.getBusinessHours());
        model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                .filter(p -> p.deceasedAt() == null)
                .toList());
        model.addAttribute("selectedDate", slotDate);
        model.addAttribute("availableSlots", appointmentService.generateAvailableSlots(slotDate));
        model.addAttribute("sortBy", "scheduledAt");
        model.addAttribute("page", appointmentService.listForApp(1, 10, currentUser, "scheduledAt"));
    }
}
