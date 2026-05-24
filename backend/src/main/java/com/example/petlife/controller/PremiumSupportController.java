package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.premium.PremiumOnlineCareForm;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/app/premium/online-care")
public class PremiumSupportController {

    private final PlanAccessService planAccessService;
    private final PetService petService;
    private final AppointmentService appointmentService;

    public PremiumSupportController(PlanAccessService planAccessService,
                                    PetService petService,
                                    AppointmentService appointmentService) {
        this.planAccessService = planAccessService;
        this.petService = petService;
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public String form(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        if (!planAccessService.canUsePrioritySupport(currentUser)) {
            throw new BadRequestException("この機能はプレミアムプランで利用できます");
        }
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new PremiumOnlineCareForm());
        }
        LocalDate slotDate = (date != null) ? date : LocalDate.now();
        model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                .filter(p -> p.deceasedAt() == null)
                .toList());
        model.addAttribute("selectedDate", slotDate);
        model.addAttribute("availableSlots", appointmentService.generateAvailableSlots(slotDate));
        return "premium/online-care";
    }

    @PostMapping
    public String create(@Valid PremiumOnlineCareForm form,
                         BindingResult result,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (!planAccessService.canUsePrioritySupport(currentUser)) {
            throw new BadRequestException("この機能はプレミアムプランで利用できます");
        }
        if (result.hasErrors()) {
            LocalDate slotDate = form.getScheduledAt() != null ? form.getScheduledAt().toLocalDate() : LocalDate.now();
            model.addAttribute("pets", petService.list(1, 100, currentUser).items().stream()
                    .filter(p -> p.deceasedAt() == null)
                    .toList());
            model.addAttribute("selectedDate", slotDate);
            model.addAttribute("availableSlots", appointmentService.generateAvailableSlots(slotDate));
            return "premium/online-care";
        }

        appointmentService.createPremiumOnlineCare(
                form.getPetId(),
                form.getScheduledAt(),
                form.getNote(),
                currentUser
        );

        ra.addFlashAttribute("success", "Zoom診療を申請しました。承認後に参加リンクを通知します。");
        return "redirect:/app/premium/online-care";
    }
}
