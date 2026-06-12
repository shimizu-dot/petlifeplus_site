package com.example.petlife.controller.line;

import com.example.petlife.service.AnnouncementService;
import com.example.petlife.service.line.LineBotService;
import com.example.petlife.service.line.LineRequestVerifier;
import com.example.petlife.service.line.LineUserLinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/line/events")
public class LineEventController {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final LineBotService lineBotService;
    private final LineRequestVerifier lineRequestVerifier;
    private final AnnouncementService announcementService;
    private final LineUserLinkService lineUserLinkService;
    private final Set<String> adminLineUserIds;
    private final JsonParser jsonParser = JsonParserFactory.getJsonParser();

    public LineEventController(LineBotService lineBotService,
                               LineRequestVerifier lineRequestVerifier,
                               AnnouncementService announcementService,
                               LineUserLinkService lineUserLinkService,
                               @Value("${admin.line-user-ids:}") String adminLineUserIdsCsv) {
        this.lineBotService = lineBotService;
        this.lineRequestVerifier = lineRequestVerifier;
        this.announcementService = announcementService;
        this.lineUserLinkService = lineUserLinkService;
        this.adminLineUserIds = Arrays.stream(adminLineUserIdsCsv.split(","))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
    }

    @PostMapping
    public ResponseEntity<?> events(
            @RequestHeader(value = "X-Line-Signature", required = false) String signature,
            @RequestBody String payloadJson
    ) {
        if (!lineRequestVerifier.isValid(signature, payloadJson)) {
            auditLog.warn("action=line_signature_invalid sigPresent={}", signature != null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ok", false));
        }

        Map<String, Object> payload = jsonParser.parseMap(payloadJson);
        Object eventsObj = payload.get("events");
        if (!(eventsObj instanceof List<?> events)) {
            return ResponseEntity.ok(Map.of("ok", true));
        }

        for (Object eventObj : events) {
            if (!(eventObj instanceof Map<?, ?> event)) continue;

            String eventType = (String) event.get("type");

            // source.userId を取得
            String senderId = null;
            Object sourceObj = event.get("source");
            if (sourceObj instanceof Map<?, ?> source) {
                senderId = (String) source.get("userId");
            }

            // --- follow イベント: 友達追加時にウェルカムメッセージを送信 ---
            if ("follow".equals(eventType)) {
                String replyToken = (String) event.get("replyToken");
                if (replyToken != null) {
                    lineBotService.replyMessage(replyToken,
                            "ペットライフプラスの LINE Bot へようこそ！\n\n" +
                            "体調の変化や気になる症状をメッセージで送ると、受診の目安をお伝えします。\n" +
                            "アプリのマイページで連携コードを取得するか、登録済みメールアドレスを送ると LINE ID を自動で設定できます。");
                }
                auditLog.info("action=line_follow userId={}", senderId);
                continue;
            }

            // --- message イベント: テキストメッセージへの返答 ---
            if (!"message".equals(eventType)) continue;

            Object msgObj = event.get("message");
            if (!(msgObj instanceof Map<?, ?> message)) continue;
            if (!"text".equals(message.get("type"))) continue;

            String replyToken = (String) event.get("replyToken");
            String text       = (String) message.get("text");

            if (replyToken == null || text == null) continue;

            // 一般ユーザー向け: 「連携 メールアドレス」で users.line_user_id を更新
            LineUserLinkService.LinkResult linkResult = lineUserLinkService.linkByMessage(senderId, text);
            if (linkResult != LineUserLinkService.LinkResult.NO_ACTION) {
                if (linkResult == LineUserLinkService.LinkResult.LINKED) {
                    lineBotService.replyMessage(replyToken, "✅ LINE連携が完了しました。");
                    auditLog.info("action=line_user_linked lineUserId={}", senderId);
                } else if (linkResult == LineUserLinkService.LinkResult.ALREADY_LINKED) {
                    lineBotService.replyMessage(replyToken,
                            "この LINE アカウントはすでに別のアカウントに連携されています。\n" +
                            "別アカウントへ切り替える場合は管理者にご連絡ください。");
                } else if (linkResult == LineUserLinkService.LinkResult.INVALID_FORMAT) {
                    lineBotService.replyMessage(replyToken,
                            "連携コマンドの形式が不正です。\n" +
                            "アプリの「LINE連携」ページで6桁のコードを取得するか、登録済みメールアドレスを送信してください。");
                } else if (linkResult == LineUserLinkService.LinkResult.TOKEN_INVALID) {
                    lineBotService.replyMessage(replyToken,
                            "コードが無効または期限切れです（有効期限: 10分）。\nアプリの「LINE連携」ページで新しいコードを取得してください。");
                } else if (linkResult == LineUserLinkService.LinkResult.USER_NOT_FOUND) {
                    lineBotService.replyMessage(replyToken,
                            "登録済みのアカウントが見つかりませんでした。\n" +
                            "メールアドレスまたは 6 桁の連携コードを確認してください。");
                }
                continue;
            }

            // 管理者からのメッセージ → お知らせ登録を試みる
            if (!adminLineUserIds.isEmpty() && adminLineUserIds.contains(senderId)) {
                if (announcementService.tryCreateFromBot(text)) {
                    lineBotService.replyMessage(replyToken, "✅ お知らせを登録・公開しました。");
                    auditLog.info("action=announcement_created_via_line user={}", senderId);
                } else if (text.strip().startsWith("お知らせ")) {
                    lineBotService.replyMessage(replyToken, AnnouncementService.usageMessage());
                } else {
                    lineBotService.replyMessage(replyToken, lineBotService.buildReply(text));
                }
            } else {
                lineBotService.replyMessage(replyToken, lineBotService.buildReply(text));
                auditLog.info("action=line_event_processed type=message");
            }
        }

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
