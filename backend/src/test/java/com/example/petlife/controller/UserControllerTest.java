package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.user.UserIntegrationStatus;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.exception.BadRequestException;
import com.example.petlife.service.PetService;
import com.example.petlife.service.PlanAccessService;
import com.example.petlife.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Test
    void editFormDefaultsGeneralUserPlanToLightWhenNoActiveSubscription() {
        UserService userService = mock(UserService.class);
        PetService petService = mock(PetService.class);
        PlanAccessService planAccessService = mock(PlanAccessService.class);
        UserController controller = new UserController(userService, petService, planAccessService);

        UserEntity entity = new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hash", null, null, null, "ACTIVE", null, null, null, null);
        when(userService.findEntity(10L)).thenReturn(entity);
        when(userService.findActivePlanNameByUserId(10L)).thenReturn(null);
        when(petService.listByOwnerUserIdForAdmin(10L)).thenReturn(List.of());
        when(planAccessService.resolveIntegrationStatusForUser(10L, 3L, null, null))
                .thenReturn(new UserIntegrationStatus(false, false, false, false, false));

        Model model = new ExtendedModelMap();
        String view = controller.editForm(10L, model, new LoginUser(1L, 1L, "管理者", "admin@example.com", "hash", true));

        assertEquals("admin/users/form", view);
        assertEquals("LIGHT", ((com.example.petlife.dto.user.UserForm) model.getAttribute("form")).getPlanTier());
    }

    @Test
    void listDisablesUserActionsForVet() {
        UserService userService = mock(UserService.class);
        PetService petService = mock(PetService.class);
        PlanAccessService planAccessService = mock(PlanAccessService.class);
        UserController controller = new UserController(userService, petService, planAccessService);

        when(userService.list(1, 10)).thenReturn(new com.example.petlife.dto.common.PageResponse<>(List.of(), 1, 10, 0));

        Model model = new ExtendedModelMap();
        String view = controller.list(1, 10, model, new LoginUser(10L, 4L, "獣医師", "vet@example.com", "hash", true));

        assertEquals("admin/users/list", view);
        assertFalse((Boolean) model.getAttribute("canManageUsers"));
    }

    @Test
    void listAllowsUserRegistrationForStaff() {
        UserService userService = mock(UserService.class);
        PetService petService = mock(PetService.class);
        PlanAccessService planAccessService = mock(PlanAccessService.class);
        UserController controller = new UserController(userService, petService, planAccessService);

        when(userService.list(1, 10)).thenReturn(new com.example.petlife.dto.common.PageResponse<>(List.of(), 1, 10, 0));

        Model model = new ExtendedModelMap();
        String view = controller.list(1, 10, model, new LoginUser(11L, 5L, "スタッフ", "staff@example.com", "hash", true));

        assertEquals("admin/users/list", view);
        assertEquals(true, model.getAttribute("canManageUsers"));
    }

    @Test
    void newFormUsesUserRegistrationMessageForVet() {
        UserService userService = mock(UserService.class);
        PetService petService = mock(PetService.class);
        PlanAccessService planAccessService = mock(PlanAccessService.class);
        UserController controller = new UserController(userService, petService, planAccessService);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> controller.newForm(new ExtendedModelMap(), new LoginUser(10L, 4L, "獣医師", "vet@example.com", "hash", true)));

        assertEquals("ユーザー登録は管理者・スタッフのみ実行できます", ex.getMessage());
    }

    @Test
    void newFormDefaultsStaffRoleToUser() {
        UserService userService = mock(UserService.class);
        PetService petService = mock(PetService.class);
        PlanAccessService planAccessService = mock(PlanAccessService.class);
        UserController controller = new UserController(userService, petService, planAccessService);

        Model model = new ExtendedModelMap();
        String view = controller.newForm(model, new LoginUser(11L, 5L, "スタッフ", "staff@example.com", "hash", true));

        assertEquals("admin/users/form", view);
        assertEquals(3L, ((com.example.petlife.dto.user.UserForm) model.getAttribute("form")).getRoleId());
    }

    @Test
    void staffCannotEditNonUserRole() {
        UserService userService = mock(UserService.class);
        PetService petService = mock(PetService.class);
        PlanAccessService planAccessService = mock(PlanAccessService.class);
        UserController controller = new UserController(userService, petService, planAccessService);

        UserEntity entity = new UserEntity(10L, 4L, "獣医師", "vet@example.com", "hash", null, null, null, "ACTIVE", null, null, null, null);
        when(userService.findEntity(10L)).thenReturn(entity);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> controller.editForm(10L, new ExtendedModelMap(), new LoginUser(11L, 5L, "スタッフ", "staff@example.com", "hash", true)));

        assertEquals("スタッフは一般ユーザーのみ編集できます", ex.getMessage());
    }

    @Test
    void vetCanOpenEditFormInReadOnlyMode() {
        UserService userService = mock(UserService.class);
        PetService petService = mock(PetService.class);
        PlanAccessService planAccessService = mock(PlanAccessService.class);
        UserController controller = new UserController(userService, petService, planAccessService);

        UserEntity entity = new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hash", null, null, null, "ACTIVE", null, null, null, null);
        when(userService.findEntity(10L)).thenReturn(entity);
        when(userService.findActivePlanNameByUserId(10L)).thenReturn(null);
        when(petService.listByOwnerUserIdForAdmin(10L)).thenReturn(List.of());
        when(planAccessService.resolveIntegrationStatusForUser(10L, 3L, null, null))
                .thenReturn(new UserIntegrationStatus(false, false, false, false, false));

        Model model = new ExtendedModelMap();
        String view = controller.editForm(10L, model, new LoginUser(12L, 4L, "獣医師", "vet@example.com", "hash", true));

        assertEquals("admin/users/form", view);
        assertEquals(true, model.getAttribute("readOnlyMode"));
    }

    @Test
    void vetCannotUpdateUser() {
        UserService userService = mock(UserService.class);
        PetService petService = mock(PetService.class);
        PlanAccessService planAccessService = mock(PlanAccessService.class);
        UserController controller = new UserController(userService, petService, planAccessService);

        UserEntity entity = new UserEntity(10L, 3L, "オーナー", "owner@example.com", "hash", null, null, null, "ACTIVE", null, null, null, null);
        when(userService.findEntity(10L)).thenReturn(entity);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> controller.update(10L, new com.example.petlife.dto.user.UserForm(), new org.springframework.validation.BeanPropertyBindingResult(new com.example.petlife.dto.user.UserForm(), "form"), new ExtendedModelMap(), new LoginUser(12L, 4L, "獣医師", "vet@example.com", "hash", true), new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap()));

        assertEquals("ユーザー編集は管理者・スタッフのみ実行できます", ex.getMessage());
    }
}
