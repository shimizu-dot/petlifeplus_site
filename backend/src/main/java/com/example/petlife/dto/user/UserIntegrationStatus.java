package com.example.petlife.dto.user;

/**
 * プランによる機能利用可否と、ユーザーのアカウント登録状況を組み合わせた統合ステータス。
 *
 * enabled  = そのユーザーの契約プランが機能を許可しているか
 * connected = 該当サービスのユーザーIDが登録済みか（Zoom はサーバーサイド管理のため登録不要）
 */
public record UserIntegrationStatus(
        boolean slackEnabled,
        boolean slackConnected,
        boolean lineEnabled,
        boolean lineConnected,
        boolean zoomEnabled
) {
    public static final String FEATURE_SLACK_BOT    = "SLACK_BOT";
    public static final String FEATURE_LINE_BOT     = "LINE_BOT";
    public static final String FEATURE_ZOOM_CONSULT = "ZOOM_CONSULT";
    public static final String FEATURE_AI_SYMPTOM   = "AI_SYMPTOM";
    public static final String FEATURE_APPOINTMENT  = "APPOINTMENT";

    public boolean slackReady()  { return slackEnabled && slackConnected; }
    public boolean lineReady()   { return lineEnabled  && lineConnected; }
}
