package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.user.UserForm;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.service.PetService;
import com.example.petlife.service.PlanAccessService;
import com.example.petlife.service.UserService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/admin/users")
public class UserController {

    private final UserService userService;
    private final PetService petService;
    private final PlanAccessService planAccessService;

    public UserController(UserService userService, PetService petService,
                          PlanAccessService planAccessService) {
        this.userService = userService;
        this.petService = petService;
        this.planAccessService = planAccessService;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model,
                       @AuthenticationPrincipal LoginUser currentUser) {
        model.addAttribute("page", userService.list(page, size));
        model.addAttribute("canManageUsers", currentUser != null && (currentUser.isAdmin() || currentUser.isStaff()));
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String newForm(Model model, @AuthenticationPrincipal LoginUser currentUser) {
        ensureCreateAccess(currentUser);
        model.addAttribute("form", buildUserFormForCurrentUser(new UserForm(), currentUser));
        model.addAttribute("editMode", false);
        return "admin/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") UserForm form,
                         BindingResult result,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        ensureCreateAccess(currentUser);
        normalizeRoleForCurrentUser(form, currentUser);
        String password = form.getPassword() == null ? "" : form.getPassword().trim();
        if (password.length() < 8 || password.length() > 64) {
            result.rejectValue("password", "Size.form.password", "パスワードは8〜64文字で入力してください");
        }
        if (result.hasErrors()) {
            model.addAttribute("form", buildUserFormForCurrentUser(form, currentUser));
            model.addAttribute("editMode", false);
            return "admin/users/form";
        }
        try {
            userService.create(form.toCreateRequest(), currentUser);
            ra.addFlashAttribute("success", "ユーザーを登録しました");
            return "redirect:/app/admin/users";
        } catch (BadRequestException ex) {
            model.addAttribute("editMode", false);
            if ("Email already exists".equals(ex.getMessage())) {
                setCreateError(model, "USR-001", "メールアドレスが重複しています。");
            } else {
                setCreateError(model, "USR-003", "ユーザー登録に失敗しました。入力内容をご確認ください。");
            }
            return "admin/users/form";
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("editMode", false);
            String detail = ex.getMostSpecificCause() != null
                    ? ex.getMostSpecificCause().getMessage()
                    : ex.getMessage();
            if (detail != null && detail.contains("users_email_key")) {
                setCreateError(model, "USR-001", "メールアドレスが重複しています。");
            } else if (detail != null && detail.contains("users_role_id_fkey")) {
                setCreateError(model, "USR-002", "ロール情報の整合性エラーです。ロール設定を確認してください。");
            } else {
                setCreateError(model, "USR-003", "ユーザー登録に失敗しました。システム管理者に連絡してください。");
            }
            return "admin/users/form";
        } catch (Exception ex) {
            model.addAttribute("editMode", false);
            setCreateError(model, "USR-003", "ユーザー登録に失敗しました。システム管理者に連絡してください。");
            return "admin/users/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model,
                           @AuthenticationPrincipal LoginUser currentUser) {
        ensureEditViewAccess(currentUser);
        UserEntity entity = userService.findEntity(id);
        ensureStaffTargetAccess(currentUser, entity.roleId());
        UserForm form = new UserForm();
        form.setRoleId(entity.roleId());
        form.setName(entity.name());
        form.setEmail(entity.email());
        form.setPhone(entity.phone());
        form.setSlackUserId(entity.slackUserId());
        form.setLineUserId(entity.lineUserId());
        form.setStatus(entity.status());
        if (entity.roleId() != null && entity.roleId() == 3L) {
            String plan = userService.findActivePlanNameByUserId(entity.id());
            form.setPlanTier(plan != null ? plan : "LIGHT");
        } else {
            form.setPlanTier("LIGHT");
        }
        form = buildUserFormForCurrentUser(form, currentUser);
        model.addAttribute("linkedPets", petService.listByOwnerUserIdForAdmin(entity.id()));
        model.addAttribute("integrationStatus",
                planAccessService.resolveIntegrationStatusForUser(
                        entity.id(), entity.roleId(), entity.slackUserId(), entity.lineUserId()));
        model.addAttribute("form",     form);
        model.addAttribute("userId",   id);
        model.addAttribute("editMode", true);
        model.addAttribute("readOnlyMode", currentUser != null && currentUser.isVet());
        return "admin/users/form";
    }

    @PatchMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") UserForm form,
                         BindingResult result,
                         Model model,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        ensureWriteAccess(currentUser);
        UserEntity existing = userService.findEntity(id);
        ensureStaffTargetAccess(currentUser, existing.roleId());
        normalizeRoleForCurrentUser(form, currentUser);
        if (result.hasErrors()) {
            model.addAttribute("form", buildUserFormForCurrentUser(form, currentUser));
            model.addAttribute("userId",   id);
            model.addAttribute("editMode", true);
            model.addAttribute("linkedPets", petService.listByOwnerUserIdForAdmin(existing.id()));
            model.addAttribute("integrationStatus",
                    planAccessService.resolveIntegrationStatusForUser(
                            existing.id(), existing.roleId(), existing.slackUserId(), existing.lineUserId()));
            return "admin/users/form";
        }
        userService.update(id, form.toUpdateRequest(), currentUser);
        ra.addFlashAttribute("success", "ユーザー情報を更新しました");
        return "redirect:/app/admin/users";
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal LoginUser currentUser,
                         RedirectAttributes ra) {
        userService.delete(id, currentUser);
        ra.addFlashAttribute("success", "ユーザーを削除しました");
        return "redirect:/app/admin/users";
    }

    /** ADMIN + STAFF のみ書き込み可。VET は一覧閲覧のみ。 */
    private void ensureCreateAccess(LoginUser currentUser) {
        if (currentUser == null || (!currentUser.isAdmin() && !currentUser.isStaff())) {
            throw new BadRequestException("ユーザー登録は管理者・スタッフのみ実行できます");
        }
    }

    private void ensureWriteAccess(LoginUser currentUser) {
        if (currentUser == null || (!currentUser.isAdmin() && !currentUser.isStaff())) {
            throw new BadRequestException("ユーザー編集は管理者・スタッフのみ実行できます");
        }
    }

    private void ensureEditViewAccess(LoginUser currentUser) {
        if (currentUser == null || (!currentUser.isAdmin() && !currentUser.isStaff() && !currentUser.isVet())) {
            throw new BadRequestException("ユーザー編集は管理者・スタッフ・獣医師のみ表示できます");
        }
    }

    private void ensureStaffTargetAccess(LoginUser currentUser, Long targetRoleId) {
        if (currentUser.isStaff() && (targetRoleId == null || targetRoleId != 3L)) {
            throw new BadRequestException("スタッフは一般ユーザーのみ編集できます");
        }
    }

    private void normalizeRoleForCurrentUser(UserForm form, LoginUser currentUser) {
        if (currentUser != null && currentUser.isStaff()) {
            form.setRoleId(3L);
        }
    }

    private UserForm buildUserFormForCurrentUser(UserForm form, LoginUser currentUser) {
        normalizeRoleForCurrentUser(form, currentUser);
        return form;
    }

    private void setCreateError(Model model, String code, String message) {
        model.addAttribute("createErrorCode", code);
        model.addAttribute("createErrorMessage", message);
    }
}
