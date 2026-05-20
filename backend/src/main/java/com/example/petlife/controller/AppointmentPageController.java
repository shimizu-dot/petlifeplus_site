package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.appointment.GeneralAppointmentForm;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.service.AppointmentService;
import com.example.petlife.service.PetService;
import com.example.petlife.service.PlanAccessService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        ensureAccessible(currentUser);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new GeneralAppointmentForm());
        }
        model.addAttribute("isAdminView", currentUser.isAdmin());
        model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                .filter(p -> p.deceasedAt() == null)
                .toList());
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
            model.addAttribute("isAdminView", false);
            model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                    .filter(p -> p.deceasedAt() == null)
                    .toList());
            model.addAttribute("page", appointmentService.listForApp(1, 10, currentUser));
            return "appointments/index";
        }
        appointmentService.createGeneralCare(form.getPetId(), form.getScheduledAt(), form.getNote(), currentUser);
        ra.addFlashAttribute("success", "診療予約を登録しました");
        return "redirect:/app/appointments";
    }

    private void ensureAccessible(LoginUser currentUser) {
        if (!currentUser.isAdmin() && !planAccessService.canUseAiSymptom(currentUser)) {
            throw new BadRequestException("この機能はスタンダード以上で利用できます");
        }
    }
}
