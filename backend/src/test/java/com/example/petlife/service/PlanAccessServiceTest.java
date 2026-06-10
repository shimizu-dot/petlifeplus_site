package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.user.UserIntegrationStatus;
import com.example.petlife.mapper.PlanFeatureMapper;
import com.example.petlife.mapper.SubscriptionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanAccessServiceTest {

    @Mock SubscriptionMapper subscriptionMapper;
    @Mock PlanFeatureMapper planFeatureMapper;

    @InjectMocks PlanAccessService svc;

    private static LoginUser normalUser() {
        return new LoginUser(1L, 3L, "オーナー", "owner@petlife.local", "hash", true);
    }

    private static LoginUser staffUser(long roleId) {
        return new LoginUser(2L, roleId, "スタッフ", "staff@petlife.local", "hash", true);
    }

    // ── resolvePlanTierByUserId ──────────────────────────────────────────────

    @Test
    void resolvesPremiumFromPlanName() {
        when(subscriptionMapper.findActivePlanNameByUserId(1L)).thenReturn("PREMIUM プラン");
        assertEquals(PlanAccessService.PlanTier.PREMIUM, svc.resolvePlanTierByUserId(1L));
    }

    @Test
    void resolvesStandardFromPlanName() {
        when(subscriptionMapper.findActivePlanNameByUserId(1L)).thenReturn("STANDARD プラン");
        assertEquals(PlanAccessService.PlanTier.STANDARD, svc.resolvePlanTierByUserId(1L));
    }

    @Test
    void defaultsToLightWhenNoPlan() {
        when(subscriptionMapper.findActivePlanNameByUserId(1L)).thenReturn(null);
        assertEquals(PlanAccessService.PlanTier.LIGHT, svc.resolvePlanTierByUserId(1L));
    }

    @Test
    void defaultsToLightWhenBlankPlan() {
        when(subscriptionMapper.findActivePlanNameByUserId(1L)).thenReturn("  ");
        assertEquals(PlanAccessService.PlanTier.LIGHT, svc.resolvePlanTierByUserId(1L));
    }

    // ── スタッフはDBを参照せず常にPREMIUM ────────────────────────────────────

    @Test
    void staffAlwaysGetsPremiumTierWithoutDbCall() {
        // VET(4), STAFF(5), ADMIN(1), SUPER(2) すべて DB 参照なし
        for (long roleId : new long[]{1L, 2L, 4L, 5L}) {
            assertEquals(PlanAccessService.PlanTier.PREMIUM,
                    svc.resolvePlanTier(staffUser(roleId)),
                    "roleId=" + roleId + " はPREMIUMであること");
        }
        verifyNoInteractions(subscriptionMapper);
    }

    @Test
    void staffAlwaysHasAllFeaturesWithoutDbCall() {
        LoginUser vet = staffUser(4L);
        assertTrue(svc.canUseAppointments(vet));
        assertTrue(svc.canUseAiSymptom(vet));
        assertTrue(svc.canUsePrioritySupport(vet));
        assertTrue(svc.canUseSlack(vet));
        assertTrue(svc.canUseLine(vet));
        verifyNoInteractions(planFeatureMapper);
    }

    // ── 一般ユーザーのフィーチャーアクセス ──────────────────────────────────

    @Test
    void lightPlanUserCannotUseAppointments() {
        when(planFeatureMapper.findActiveFeatureCodesByUserId(1L)).thenReturn(Set.of());
        assertFalse(svc.canUseAppointments(normalUser()));
    }

    @Test
    void nullFeatureSetIsTreatedAsNoFeatures() {
        when(planFeatureMapper.findActiveFeatureCodesByUserId(1L)).thenReturn(null);
        assertFalse(svc.canUseAiSymptom(normalUser()));
        assertFalse(svc.canUsePrioritySupport(normalUser()));
    }

    @Test
    void standardPlanUserCanUseAppointments() {
        when(planFeatureMapper.findActiveFeatureCodesByUserId(1L))
                .thenReturn(Set.of(UserIntegrationStatus.FEATURE_APPOINTMENT));
        assertTrue(svc.canUseAppointments(normalUser()));
    }

    @Test
    void premiumPlanUserCanUseAllFeatures() {
        when(planFeatureMapper.findActiveFeatureCodesByUserId(1L))
                .thenReturn(Set.of(
                        UserIntegrationStatus.FEATURE_APPOINTMENT,
                        UserIntegrationStatus.FEATURE_AI_SYMPTOM,
                        UserIntegrationStatus.FEATURE_ZOOM_CONSULT,
                        UserIntegrationStatus.FEATURE_SLACK_BOT,
                        UserIntegrationStatus.FEATURE_LINE_BOT));
        assertTrue(svc.canUseAppointments(normalUser()));
        assertTrue(svc.canUseAiSymptom(normalUser()));
        assertTrue(svc.canUsePrioritySupport(normalUser()));
    }

    // ── resolveIntegrationStatus ─────────────────────────────────────────────

    @Test
    void integrationStatusReflectsLineRegistration() {
        when(planFeatureMapper.findActiveFeatureCodesByUserId(1L))
                .thenReturn(Set.of(UserIntegrationStatus.FEATURE_LINE_BOT));
        UserIntegrationStatus status = svc.resolveIntegrationStatus(normalUser(), null, "U_LINE_123");
        assertTrue(status.lineEnabled());
        assertTrue(status.lineConnected());
        assertTrue(status.lineReady());
    }

    @Test
    void integrationStatusIsNotReadyWhenLineNotRegistered() {
        when(planFeatureMapper.findActiveFeatureCodesByUserId(1L))
                .thenReturn(Set.of(UserIntegrationStatus.FEATURE_LINE_BOT));
        UserIntegrationStatus status = svc.resolveIntegrationStatus(normalUser(), null, null);
        assertTrue(status.lineEnabled());
        assertFalse(status.lineConnected());
        assertFalse(status.lineReady());
    }
}
