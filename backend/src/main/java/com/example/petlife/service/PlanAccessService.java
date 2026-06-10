package com.example.petlife.service;

import com.example.petlife.config.LoginUser;
import com.example.petlife.dto.user.UserIntegrationStatus;
import com.example.petlife.mapper.PlanFeatureMapper;
import com.example.petlife.mapper.SubscriptionMapper;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class PlanAccessService {

    public enum PlanTier {
        LIGHT, STANDARD, PREMIUM
    }

    private static final Set<String> ALL_FEATURES = Set.of(
            UserIntegrationStatus.FEATURE_AI_SYMPTOM,
            UserIntegrationStatus.FEATURE_SLACK_BOT,
            UserIntegrationStatus.FEATURE_LINE_BOT,
            UserIntegrationStatus.FEATURE_ZOOM_CONSULT,
            UserIntegrationStatus.FEATURE_APPOINTMENT
    );

    private final SubscriptionMapper subscriptionMapper;
    private final PlanFeatureMapper planFeatureMapper;

    public PlanAccessService(SubscriptionMapper subscriptionMapper, PlanFeatureMapper planFeatureMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.planFeatureMapper = planFeatureMapper;
    }

    // ── Plan tier (label/display) ────────────────────────────────────────────

    public PlanTier resolvePlanTier(LoginUser user) {
        if (user == null || user.hasStaffAccess()) {
            return PlanTier.PREMIUM;
        }
        return resolvePlanTierByUserId(user.id());
    }

    public PlanTier resolvePlanTierByUserId(Long userId) {
        String raw = subscriptionMapper.findActivePlanNameByUserId(userId);
        if (raw == null || raw.isBlank()) return PlanTier.LIGHT;
        if (raw.contains("PREMIUM")) return PlanTier.PREMIUM;
        if (raw.contains("STANDARD")) return PlanTier.STANDARD;
        return PlanTier.LIGHT;
    }

    public String planLabel(LoginUser user) {
        return switch (resolvePlanTier(user)) {
            case LIGHT -> "ライト";
            case STANDARD -> "スタンダード";
            case PREMIUM -> "プレミアム";
        };
    }

    public String planLabelByUserId(Long userId) {
        return switch (resolvePlanTierByUserId(userId)) {
            case LIGHT -> "ライト";
            case STANDARD -> "スタンダード";
            case PREMIUM -> "プレミアム";
        };
    }

    public String planLabelEnByUserId(Long userId) {
        return switch (resolvePlanTierByUserId(userId)) {
            case LIGHT -> "Light";
            case STANDARD -> "Standard";
            case PREMIUM -> "Premium";
        };
    }

    // ── Feature access (plan_features table) ────────────────────────────────

    /**
     * ユーザーの有効プランに紐づく機能コード一覧を返す。
     * ADMIN / VET / STAFF は全機能利用可能として扱う。
     */
    private Set<String> activeFeatures(LoginUser user) {
        if (user == null || user.hasStaffAccess()) {
            return ALL_FEATURES;
        }
        Set<String> features = planFeatureMapper.findActiveFeatureCodesByUserId(user.id());
        return features == null ? Set.of() : features;
    }

    public boolean canUseAiSymptom(LoginUser user) {
        return activeFeatures(user).contains(UserIntegrationStatus.FEATURE_AI_SYMPTOM);
    }

    public boolean canUseAppointments(LoginUser user) {
        return activeFeatures(user).contains(UserIntegrationStatus.FEATURE_APPOINTMENT);
    }

    public boolean canUsePrioritySupport(LoginUser user) {
        return activeFeatures(user).contains(UserIntegrationStatus.FEATURE_ZOOM_CONSULT);
    }

    public boolean canUseSlack(LoginUser user) {
        return activeFeatures(user).contains(UserIntegrationStatus.FEATURE_SLACK_BOT);
    }

    public boolean canUseLine(LoginUser user) {
        return activeFeatures(user).contains(UserIntegrationStatus.FEATURE_LINE_BOT);
    }

    public boolean canUseZoom(LoginUser user) {
        return activeFeatures(user).contains(UserIntegrationStatus.FEATURE_ZOOM_CONSULT);
    }

    // ── Integration status (feature availability + registration) ────────────

    /**
     * プランによる機能可否とアカウント登録状況を組み合わせたステータスを返す。
     *
     * @param user        認証済みログインユーザー
     * @param slackUserId users.slack_user_id の値
     * @param lineUserId  users.line_user_id の値
     */
    public UserIntegrationStatus resolveIntegrationStatus(LoginUser user, String slackUserId, String lineUserId) {
        Set<String> features = activeFeatures(user);
        return new UserIntegrationStatus(
                features.contains(UserIntegrationStatus.FEATURE_SLACK_BOT),
                slackUserId != null && !slackUserId.isBlank(),
                features.contains(UserIntegrationStatus.FEATURE_LINE_BOT),
                lineUserId != null && !lineUserId.isBlank(),
                features.contains(UserIntegrationStatus.FEATURE_ZOOM_CONSULT)
        );
    }

    /**
     * ロール込みの統合ステータス解決。
     * roleId が 3（一般ユーザー）以外のスタッフ系ロールは全機能利用可能として扱う。
     */
    public UserIntegrationStatus resolveIntegrationStatusForUser(
            Long userId, Long roleId, String slackUserId, String lineUserId) {
        boolean isStaffRole = roleId != null && roleId != 3L;
        Set<String> features = isStaffRole ? ALL_FEATURES : planFeatureMapper.findActiveFeatureCodesByUserId(userId);
        if (features == null) {
            features = Set.of();
        }
        return new UserIntegrationStatus(
                features.contains(UserIntegrationStatus.FEATURE_SLACK_BOT),
                slackUserId != null && !slackUserId.isBlank(),
                features.contains(UserIntegrationStatus.FEATURE_LINE_BOT),
                lineUserId != null && !lineUserId.isBlank(),
                features.contains(UserIntegrationStatus.FEATURE_ZOOM_CONSULT)
        );
    }
}
