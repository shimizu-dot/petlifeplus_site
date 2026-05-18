package com.example.petlife.controller;

import com.example.petlife.dto.user.UserForm;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       Model model) {
        model.addAttribute("page", userService.list(page, size));
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new UserForm());
        model.addAttribute("editMode", false);
        return "admin/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") UserForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("editMode", false);
            return "admin/users/form";
        }
        userService.create(form.toCreateRequest());
        ra.addFlashAttribute("success", "ユーザーを登録しました");
        return "redirect:/app/admin/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UserEntity entity = userService.findEntity(id);
        UserForm form = new UserForm();
        form.setRoleId(entity.roleId());
        form.setName(entity.name());
        form.setEmail(entity.email());
        form.setPhone(entity.phone());
        form.setStatus(entity.status());
        if (entity.roleId() != null && entity.roleId() == 2L) {
            String plan = userService.findActivePlanNameByUserId(entity.id());
            form.setPlanTier(plan != null ? plan : "LIGHT");
        }
        model.addAttribute("form",     form);
        model.addAttribute("userId",   id);
        model.addAttribute("editMode", true);
        return "admin/users/form";
    }

    @PatchMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") UserForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes ra) {
        if (result.hasErrors()) {
            model.addAttribute("userId",   id);
            model.addAttribute("editMode", true);
            return "admin/users/form";
        }
        userService.update(id, form.toUpdateRequest());
        ra.addFlashAttribute("success", "ユーザー情報を更新しました");
        return "redirect:/app/admin/users";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        userService.delete(id);
        ra.addFlashAttribute("success", "ユーザーを削除しました");
        return "redirect:/app/admin/users";
    }
}
