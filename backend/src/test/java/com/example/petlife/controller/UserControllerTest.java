package com.example.petlife.controller;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.user.UserIntegrationStatus;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.service.PetService;
import com.example.petlife.service.PlanAccessService;
import com.example.petlife.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
