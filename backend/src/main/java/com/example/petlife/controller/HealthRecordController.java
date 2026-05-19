package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.health.HealthRecordForm;
import com.example.petlife.entity.HealthRecordEntity;
import com.example.petlife.service.HealthRecordService;
import com.example.petlife.service.PetService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequestMapping("/app/pets/{petId}/health-records")
public class HealthRecordController {

    private final HealthRecordService healthRecordService;
    private final PetService petService;

    public HealthRecordController(HealthRecordService healthRecordService, PetService petService) {
        this.healthRecordService = healthRecordService;
        this.petService = petService;
    }

    @GetMapping
    public String list(@PathVariable Long petId,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) LocalDate recordDate,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("pet",  petService.get(petId, currentUser));
        model.addAttribute("page", healthRecordService.listForPet(petId, page, size, recordDate, currentUser));
        model.addAttribute("recordDate", recordDate);
        return "health/list";
    }

    @GetMapping("/new")
    public String newForm(@PathVariable Long petId,
                          Model model,
                          @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("pet",      petService.get(petId, currentUser));
        model.addAttribute("form",     new HealthRecordForm());
        model.addAttribute("editMode", false);
        return "health/form";
    }

    @PostMapping
    public String create(@PathVariable Long petId,
                         @Valid @ModelAttribute("form") HealthRecordForm form,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         BindingResult result,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (form.getRecordDate() == null) {
            result.rejectValue("recordDate", "NotNull.form.recordDate", "記録日は必須です");
        } else if (isFutureInJst(form.getRecordDate())) {
            result.rejectValue("recordDate", "PastOrPresent.form.recordDate", "現在もしくは過去の日付にしてください");
        }
        if (result.hasErrors()) {
            model.addAttribute("pet",      petService.get(petId, currentUser));
            model.addAttribute("editMode", false);
            return "health/form";
        }
        healthRecordService.create(
                petId, form.toCreateRequest(petId, currentUser.id(), null), imageFile, currentUser);
        ra.addFlashAttribute("success", "健康記録を登録しました");
        return "redirect:/app/pets/" + petId + "/health-records";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long petId,
                           @PathVariable Long id,
                           Model model,
                           @AuthenticationPrincipal LoginUser currentUser) {
        HealthRecordEntity entity = healthRecordService.getEntity(id, petId, currentUser);
        HealthRecordForm form = new HealthRecordForm();
        form.setRecordDate(entity.recordDate());
        form.setWeightKg(entity.weightKg());
        form.setMealMemo(entity.mealMemo());
        form.setExerciseMinutes(entity.exerciseMinutes());
        form.setMealScore(entity.mealScore());
        form.setExerciseScore(entity.exerciseScore());
        form.setSleepScore(entity.sleepScore());
        form.setMoodScore(entity.moodScore());
        form.setOverallScore(entity.overallScore());
        form.setNote(entity.note());
        model.addAttribute("pet",      petService.get(petId, currentUser));
        model.addAttribute("form",     form);
        model.addAttribute("recordImagePath", entity.imagePath());
        model.addAttribute("recordId", id);
        model.addAttribute("editMode", true);
        return "health/form";
    }

    @PatchMapping("/{id}")
    public String update(@PathVariable Long petId,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("form") HealthRecordForm form,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         BindingResult result,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        LocalDate existingDate = healthRecordService.getEntity(id, petId, currentUser).recordDate();
        // 編集時は記録日変更不可: 送信値に関わらず既存値を維持
        form.setRecordDate(existingDate);
        if (result.hasErrors()) {
            model.addAttribute("pet",      petService.get(petId, currentUser));
            model.addAttribute("recordId", id);
            model.addAttribute("editMode", true);
            return "health/form";
        }
        String currentImagePath = healthRecordService.getEntity(id, petId, currentUser).imagePath();
        healthRecordService.update(id, petId, form.toUpdateRequest(currentImagePath), imageFile, currentUser);
        ra.addFlashAttribute("success", "健康記録を更新しました");
        return "redirect:/app/pets/" + petId + "/health-records";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long petId,
                         @PathVariable Long id,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        healthRecordService.delete(id, petId, currentUser);
        ra.addFlashAttribute("success", "健康記録を削除しました");
        return "redirect:/app/pets/" + petId + "/health-records";
    }

    private boolean isFutureInJst(LocalDate date) {
        LocalDate todayJst = LocalDate.now(ZoneId.of("Asia/Tokyo"));
        return date != null && date.isAfter(todayJst);
    }
}
