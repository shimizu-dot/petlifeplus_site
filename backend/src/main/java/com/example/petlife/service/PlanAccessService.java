package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.mapper.SubscriptionMapper;
import org.springframework.stereotype.Service;

@Service
public class PlanAccessService {

    public enum PlanTier {
        LIGHT, STANDARD, PREMIUM
    }

    private final SubscriptionMapper subscriptionMapper;

    public PlanAccessService(SubscriptionMapper subscriptionMapper) {
        this.subscriptionMapper = subscriptionMapper;
    }

    public PlanTier resolvePlanTier(LoginUser user) {
        if (user == null || user.isAdmin()) {
            return PlanTier.PREMIUM;
        }
        String raw = subscriptionMapper.findActivePlanNameByUserId(user.id());
        if (raw == null || raw.isBlank()) {
            return PlanTier.LIGHT;
        }
        if (raw.contains("PREMIUM")) {
            return PlanTier.PREMIUM;
        }
        if (raw.contains("STANDARD")) {
            return PlanTier.STANDARD;
        }
        return PlanTier.LIGHT;
    }

    public boolean canUseAiSymptom(LoginUser user) {
        PlanTier tier = resolvePlanTier(user);
        return tier == PlanTier.STANDARD || tier == PlanTier.PREMIUM;
    }

    public boolean canUsePrioritySupport(LoginUser user) {
        return resolvePlanTier(user) == PlanTier.PREMIUM;
    }

    public String planLabel(LoginUser user) {
        return switch (resolvePlanTier(user)) {
            case LIGHT -> "ライト";
            case STANDARD -> "スタンダード";
            case PREMIUM -> "プレミアム";
        };
    }
}
