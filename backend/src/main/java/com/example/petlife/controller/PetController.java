package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.pet.PetForm;
import com.example.petlife.dto.pet.PetResponse;
import com.example.petlife.entity.PetEntity;
import com.example.petlife.service.PetService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("page", petService.list(page, size, currentUser));
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
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("editMode", false);
            return "pets/form";
        }
        PetResponse pet = petService.create(form.toCreateRequest(currentUser.id()), currentUser);
        ra.addFlashAttribute("success", "ペットを登録しました");
        return "redirect:/app/pets/" + pet.id();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("pet", petService.get(id, currentUser));
        return "pets/detail";
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
        model.addAttribute("petId", id);
        model.addAttribute("editMode", true);
        return "pets/form";
    }

    @PatchMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") PetForm form,
                         BindingResult result,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("petId", id);
            model.addAttribute("editMode", true);
            return "pets/form";
        }
        petService.update(id, form.toUpdateRequest(), currentUser);
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
}
