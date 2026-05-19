package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.pet.PetCareRecordForm;
import com.example.petlife.dto.pet.PetForm;
import com.example.petlife.dto.pet.PetResponse;
import com.example.petlife.dto.symptom.SymptomCheckForm;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.service.PlanAccessService;
import com.example.petlife.service.PetCareRecordService;
import com.example.petlife.service.PetService;
import com.example.petlife.service.SymptomCheckService;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequestMapping("/app/pets")
public class PetController {

    private final PetService petService;
    private final PetCareRecordService petCareRecordService;
    private final SymptomCheckService symptomCheckService;
    private final PlanAccessService planAccessService;
    private final UserService userService;

    public PetController(PetService petService, PetCareRecordService petCareRecordService,
                         SymptomCheckService symptomCheckService, PlanAccessService planAccessService,
                         UserService userService) {
        this.petService = petService;
        this.petCareRecordService = petCareRecordService;
        this.symptomCheckService = symptomCheckService;
        this.planAccessService = planAccessService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        var petPage = petService.list(page, size, currentUser);
        model.addAttribute("page", petPage);
        if (currentUser.canManagePets()) {
            Map<Long, String> petPlanLabels = new HashMap<>();
            for (PetResponse pet : petPage.items()) {
                petPlanLabels.put(pet.id(), planAccessService.planLabelEnByUserId(pet.ownerUserId()));
            }
            model.addAttribute("petPlanLabels", petPlanLabels);
        }
        return "pets/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new PetForm());
        model.addAttribute("editMode", false);
        return "pets/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") PetForm form,
                         BindingResult result,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        validateBirthDate(form, result);
        if (result.hasErrors()) {
            model.addAttribute("editMode", false);
            return "pets/form";
        }
        PetResponse pet = petService.create(form.toCreateRequest(currentUser.id()), imageFile, currentUser);
        ra.addFlashAttribute("success", "ペットを登録しました");
        return "redirect:/app/pets/" + pet.id();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser) {
        PetResponse pet = petService.get(id, currentUser);
        model.addAttribute("pet", pet);
        if (currentUser.canManagePets()) {
            model.addAttribute("ownerName", userService.get(pet.ownerUserId()).name());
        }
        model.addAttribute("careForm", new PetCareRecordForm());
        model.addAttribute("symptomForm", new SymptomCheckForm());
        model.addAttribute("careRecords", petCareRecordService.listByPet(id, currentUser));
        model.addAttribute("upcomingCareNotices", petCareRecordService.listUpcomingNotices(id, currentUser));
        model.addAttribute("symptomChecks", symptomCheckService.recentByPet(id, currentUser));
        return "pets/detail";
    }

    @PostMapping("/{id}/symptom-check")
    public String symptomCheck(@PathVariable Long id,
                               @Valid @ModelAttribute("symptomForm") SymptomCheckForm form,
                               BindingResult result,
                               Model model,
                               @AuthenticationPrincipal LoginUser currentUser,
                               RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("pet", petService.get(id, currentUser));
            model.addAttribute("careForm", new PetCareRecordForm());
            model.addAttribute("careRecords", petCareRecordService.listByPet(id, currentUser));
            model.addAttribute("upcomingCareNotices", petCareRecordService.listUpcomingNotices(id, currentUser));
            model.addAttribute("symptomChecks", symptomCheckService.recentByPet(id, currentUser));
            return "pets/detail";
        }
        try {
            symptomCheckService.runCheck(id, form, currentUser);
            ra.addFlashAttribute("success", "AI症状チェックを実行しました");
        } catch (BadRequestException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/app/pets/" + id;
    }

    @PostMapping("/{id}/care-records")
    public String addCareRecord(@PathVariable Long id,
                                @ModelAttribute("careForm") PetCareRecordForm form,
                                @AuthenticationPrincipal LoginUser currentUser,
                                RedirectAttributes ra) {
        petCareRecordService.addRecord(id, form, currentUser);
        ra.addFlashAttribute("success", "ワクチン・診療記録を追加しました");
        return "redirect:/app/pets/" + id;
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Model model,
                           @AuthenticationPrincipal LoginUser currentUser) {
        PetEntity entity = petService.getEntity(id, currentUser);
        PetForm form = new PetForm();
        form.setName(entity.name());
        form.setSpecies(entity.species());
        form.setBreed(entity.breed());
        form.setSex(entity.sex());
        form.setBirthDate(entity.birthDate());
        form.setWeightBaselineKg(entity.weightBaselineKg());
        model.addAttribute("form", form);
        model.addAttribute("petImagePath", entity.imagePath());
        model.addAttribute("petId", id);
        model.addAttribute("editMode", true);
        return "pets/form";
    }

    @PatchMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") PetForm form,
                         BindingResult result,
                         @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        validateBirthDate(form, result);
        if (result.hasErrors()) {
            model.addAttribute("petId", id);
            model.addAttribute("editMode", true);
            return "pets/form";
        }
        String currentImagePath = petService.getEntity(id, currentUser).imagePath();
        petService.update(id, form.toUpdateRequest(currentImagePath), imageFile, currentUser);
        ra.addFlashAttribute("success", "ペット情報を更新しました");
        return "redirect:/app/pets/" + id;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        petService.delete(id, currentUser);
        ra.addFlashAttribute("success", "ペットを削除しました");
        return "redirect:/app/pets";
    }

    private void validateBirthDate(PetForm form, BindingResult result) {
        LocalDate birthDate = form.getBirthDate();
        if (birthDate != null && birthDate.isAfter(LocalDate.now(ZoneId.of("Asia/Tokyo")))) {
            result.rejectValue("birthDate", "PastOrPresent.form.birthDate", "生年月日は現在日以前を入力してください");
        }
    }
}
