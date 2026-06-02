package com.example.petlife.service;

import com.example.petlife.dto.billing.InvoiceRow;
import com.example.petlife.entity.NotificationEntity;
import com.example.petlife.mapper.NotificationMapper;
import com.example.petlife.service.line.LineBotService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class BillingNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BillingNotificationService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");

    private final JavaMailSender mailSender;
    private final LineBotService lineBotService;
    private final NotificationMapper notificationMapper;

    @Value("${sendgrid.from-email:noreply@petlife.local}")
    private String fromEmail;

    @Value("${sendgrid.from-name:ペットライフプラス}")
    private String fromName;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    // 振込先情報
    @Value("${billing.bank-name:〇〇銀行}")
    private String bankName;

    @Value("${billing.bank-branch:△△支店}")
    private String bankBranch;

    @Value("${billing.bank-account-number:12345678}")
    private String bankAccountNumber;

    @Value("${billing.bank-account-holder:PetLife 事務局}")
    private String bankAccountHolder;

    // PayPay 情報
    @Value("${billing.paypay-qr-url:}")
    private String paypayQrUrl;

    @Value("${billing.paypay-id:}")
    private String paypayId;

    public BillingNotificationService(JavaMailSender mailSender,
                                      LineBotService lineBotService,
                                      NotificationMapper notificationMapper) {
        this.mailSender = mailSender;
        this.lineBotService = lineBotService;
        this.notificationMapper = notificationMapper;
    }

    /**
     * アプリ内通知のみ同期送信（呼び出し元スレッドで実行 — 失敗がすぐ検知できる）。
     * BillingService.createInvoice() / registerPayment() から直接呼ばれる。
     */
    public void sendInvoiceIssuedInApp(InvoiceRow invoice) {
        sendInAppNotification(
                invoice.ownerUserId(),
                "💳 お支払いのご案内（" + invoice.invoiceNumber() + "）",
                buildInvoiceIssuedText(invoice)
        );
    }

    public void sendPaymentConfirmedInApp(InvoiceRow invoice) {
        sendInAppNotification(
                invoice.ownerUserId(),
                "✅ お支払い確認のご連絡（" + invoice.invoiceNumber() + "）",
                buildPaymentConfirmedText(invoice)
        );
    }

    /** 支払期限超過 — アプリ内通知（同期） */
    public void sendOverdueInApp(InvoiceRow invoice) {
        sendInAppNotification(
                invoice.ownerUserId(),
                "⚠️ サービス停止のお知らせ（" + invoice.invoiceNumber() + "）",
                buildOverdueText(invoice)
        );
    }

    /** 支払期限超過 — メール + LINE（非同期） */
    @Async
    public void notifyOverdueAsync(InvoiceRow invoice) {
        String subject = "【ペットライフプラス】サービス停止のお知らせ（" + invoice.invoiceNumber() + "）";
        sendEmail(invoice.ownerEmail(), invoice.ownerName(), subject, buildOverdueHtml(invoice));
        sendLine(invoice.lineUserId(), buildOverdueText(invoice));
    }

    /** メール + LINE のみ非同期送信（遅延・失敗してもアプリ内通知には影響しない） */
    @Async
    public void notifyInvoiceIssued(InvoiceRow invoice) {
        String subject = "【ペットライフプラス】お支払いのご案内（" + invoice.invoiceNumber() + "）";
        sendEmail(invoice.ownerEmail(), invoice.ownerName(), subject, buildInvoiceIssuedHtml(invoice));
        sendLine(invoice.lineUserId(), buildInvoiceIssuedLine(invoice));
    }

    /** 入金確認完了 — メールのみ非同期 */
    @Async
    public void notifyPaymentConfirmed(InvoiceRow invoice) {
        String subject = "【ペットライフプラス】お支払い確認のご連絡（" + invoice.invoiceNumber() + "）";
        sendEmail(invoice.ownerEmail(), invoice.ownerName(), subject, buildPaymentConfirmedHtml(invoice));
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private void sendEmail(String toEmail, String toName, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Failed to send billing email to {}: {}", toEmail, e.getMessage());
        }
    }

    private void sendLine(String lineUserId, String text) {
        if (lineUserId == null || lineUserId.isBlank()) return;
        try {
            lineBotService.pushMessage(lineUserId, text);
        } catch (Exception e) {
            log.warn("Failed to send billing LINE message to {}: {}", lineUserId, e.getMessage());
        }
    }

    private void sendInAppNotification(Long userId, String title, String body) {
        if (userId == null) return;
        try {
            NotificationEntity notification = new NotificationEntity(
                    null, "INFO", title, body,
                    null, LocalDateTime.now(), "SENT",
                    userId, null, null, null
            );
            Long notificationId = notificationMapper.insertReturningId(notification);
            if (notificationId == null) {
                log.warn("Failed to insert billing notification for user {}", userId);
                return;
            }
            notificationMapper.insertRecipient(notificationId, userId);
            notificationMapper.updateRecipientStatus(notificationId, userId, "SENT");
        } catch (Exception e) {
            log.warn("Failed to create in-app billing notification for user {}: {}", userId, e.getMessage());
        }
    }

    private String formatAmount(BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.JAPAN).format(amount);
    }

    /** 請求書ビュー URL */
    private String invoiceUrl(InvoiceRow inv) {
        return appBaseUrl + invoicePath(inv);
    }

    /** 請求書ビュー相対パス（アプリ内通知/LINE向け） */
    private String invoicePath(InvoiceRow inv) {
        return "/app/invoices/" + inv.invoiceId();
    }

    private String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        // 典型的な文字化けパターンが含まれる場合は安全な既定値にフォールバック
        if (value.contains("ã")
                || value.contains("â")
                || value.contains("Ã")
                || value.contains("Â")
                || value.contains("ä")
                || value.contains("å")
                || value.contains("�")) {
            return fallback;
        }
        return value;
    }

    /** 請求通知のプレーンテキスト本文（アプリ内通知・LINE 共通） */
    private String buildInvoiceIssuedText(InvoiceRow inv) {
        String safeBankName = safeText(bankName, "〇〇銀行");
        String safeBankBranch = safeText(bankBranch, "△△支店");
        String safeBankAccountHolder = safeText(bankAccountHolder, "PetLife 事務局");
        String paypayLine = !paypayId.isBlank()
                ? "PayPay ID: " + paypayId
                : (!paypayQrUrl.isBlank() ? "PayPay QR: " + paypayQrUrl : "詳細は管理者にご確認ください");

        return inv.planName() + "プランの申請を受け付けました。\n"
                + "金額: ¥" + formatAmount(inv.amount()) + "\n"
                + "お支払期限: " + inv.dueDate().format(DATE_FMT) + "\n"
                + "\n"
                + "■ 銀行振り込み\n"
                + "　" + safeBankName + "　" + safeBankBranch + "\n"
                + "　口座番号：" + bankAccountNumber + "\n"
                + "　名義：" + safeBankAccountHolder + "\n"
                + "\n"
                + "■ PayPay\n"
                + "　" + paypayLine + "\n"
                + "\n"
                + "📄 請求書ダウンロード: " + invoicePath(inv);
    }

    /** 入金確認のプレーンテキスト本文（アプリ内通知共通） */
    private String buildPaymentConfirmedText(InvoiceRow inv) {
        String validity = inv.subscriptionEndDate() != null
                ? inv.subscriptionEndDate().format(DATE_FMT) + " まで有効になりました。"
                : "引き続きご利用いただけます。";

        return "契約料金のお支払いを確認しました。\n"
                + validity + "\n"
                + "\n"
                + "請求番号: " + inv.invoiceNumber() + "\n"
                + "プラン: " + inv.planName() + "\n"
                + "金額: ¥" + formatAmount(inv.amount()) + "\n"
                + "\n"
                + "🧾 領収書ダウンロード: " + invoicePath(inv);
    }

    private String buildInvoiceIssuedHtml(InvoiceRow inv) {
        String safeBankName = safeText(bankName, "〇〇銀行");
        String safeBankBranch = safeText(bankBranch, "△△支店");
        String safeBankAccountHolder = safeText(bankAccountHolder, "PetLife 事務局");
        // PayPay セクション — QR 画像があれば <img>、ID があればテキスト、なければ案内文
        String paypaySection;
        if (!paypayQrUrl.isBlank()) {
            paypaySection = "<strong>② PayPay</strong><br>"
                    + "<img src=\"" + paypayQrUrl + "\" alt=\"PayPay QR\" "
                    + "style=\"width:160px;height:160px;margin-top:8px;display:block;\">";
        } else if (!paypayId.isBlank()) {
            paypaySection = "<strong>② PayPay</strong><br>"
                    + "PayPay ID: <strong>" + paypayId + "</strong>";
        } else {
            paypaySection = "<strong>② PayPay</strong><br>詳細は管理者よりご案内します。";
        }

        return """
            <!DOCTYPE html>
            <html lang="ja">
            <head><meta charset="UTF-8">
            <style>
              body{font-family:'Noto Sans JP',sans-serif;background:#f8f9fa;margin:0;padding:20px;}
              .wrap{max-width:560px;margin:auto;background:#fff;border-radius:12px;padding:32px;box-shadow:0 4px 12px rgba(0,0,0,.08);}
              h2{color:#1d4ed8;margin-top:0;}
              .info-box{background:#f0f7ff;border-radius:8px;padding:16px;margin:16px 0;}
              .row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #e5e7eb;}
              .row:last-child{border-bottom:none;}
              .label{color:#6b7280;font-size:14px;}
              .value{font-weight:700;color:#111827;}
              .amount{color:#1d4ed8;font-size:18px;}
              .methods{background:#fefce8;border:1px solid #fde68a;border-radius:8px;padding:14px;margin:16px 0;}
              .method-block{margin-bottom:16px;}
              .method-block:last-child{margin-bottom:0;}
              .footer{font-size:12px;color:#9ca3af;margin-top:24px;text-align:center;}
            </style>
            </head>
            <body>
            <div class="wrap">
              <h2>💳 お支払いのご案内</h2>
              <p>%s 様、いつもペットライフプラスをご利用いただきありがとうございます。<br>
              下記の通り請求書を発行しましたのでご確認ください。</p>
              <div class="info-box">
                <div class="row"><span class="label">請求番号</span><span class="value">%s</span></div>
                <div class="row"><span class="label">プラン</span><span class="value">%s</span></div>
                <div class="row"><span class="label">請求金額</span><span class="value amount">¥%s</span></div>
                <div class="row"><span class="label">お支払期限</span><span class="value">%s</span></div>
              </div>
              <div class="methods">
                <strong>💰 お支払い方法</strong><br><br>
                <div class="method-block">
                  <strong>① 銀行振り込み</strong><br>
                  %s　%s<br>
                  口座番号：%s<br>
                  名義：%s
                </div>
                <div class="method-block">
                  %s
                </div>
              </div>
              <p>お支払いが完了しましたら確認メールをお送りします。<br>
              ご不明な点はお気軽にお問い合わせください。</p>
              <div style="text-align:center;margin:20px 0;">
                <a href="%s" style="display:inline-block;background:#1d4ed8;color:#fff;padding:10px 24px;border-radius:8px;text-decoration:none;font-weight:700;">📄 請求書を確認する</a>
              </div>
              <div class="footer">ペットライフプラス | 自動送信メールのため返信はご遠慮ください</div>
            </div>
            </body></html>
            """.formatted(
                inv.ownerName(), inv.invoiceNumber(), inv.planName(),
                formatAmount(inv.amount()), inv.dueDate().format(DATE_FMT),
                safeBankName, safeBankBranch, bankAccountNumber, safeBankAccountHolder,
                paypaySection, invoiceUrl(inv));
    }

    private String buildInvoiceIssuedLine(InvoiceRow inv) {
        String safeBankName = safeText(bankName, "〇〇銀行");
        String safeBankBranch = safeText(bankBranch, "△△支店");
        String safeBankAccountHolder = safeText(bankAccountHolder, "PetLife 事務局");
        String paypayLine = !paypayId.isBlank()
                ? "PayPay ID: " + paypayId
                : (!paypayQrUrl.isBlank() ? "PayPay QR: " + paypayQrUrl : "詳細は管理者よりご案内します");

        return ("【ペットライフプラス】お支払いのご案内\n\n"
                + "請求番号: " + inv.invoiceNumber() + "\n"
                + "プラン: " + inv.planName() + "\n"
                + "金額: ¥" + formatAmount(inv.amount()) + "\n"
                + "お支払期限: " + inv.dueDate().format(DATE_FMT) + "\n\n"
                + "■ 銀行振り込み\n"
                + safeBankName + "　" + safeBankBranch + "\n"
                + "口座番号：" + bankAccountNumber + "\n"
                + "名義：" + safeBankAccountHolder + "\n\n"
                + "■ PayPay\n"
                + paypayLine + "\n\n"
                + "📄 請求書: " + invoicePath(inv)).strip();
    }

    private String buildOverdueText(InvoiceRow inv) {
        return "期日までにお支払いが確認できなかったので、サービスの提供を停止します。\n"
                + "既存のデータに関し、一切の保証は致しません。\n"
                + "サービスの再開をご希望の場合は、あらためて、運営事務局まで、お問合せください。\n"
                + "\n"
                + "請求番号: " + inv.invoiceNumber() + "\n"
                + "プラン: " + inv.planName() + "\n"
                + "金額: ¥" + formatAmount(inv.amount()) + "\n"
                + "お支払期限: " + inv.dueDate().format(DATE_FMT);
    }

    private String buildOverdueHtml(InvoiceRow inv) {
        return """
            <!DOCTYPE html>
            <html lang="ja">
            <head><meta charset="UTF-8">
            <style>
              body{font-family:'Noto Sans JP',sans-serif;background:#f8f9fa;margin:0;padding:20px;}
              .wrap{max-width:560px;margin:auto;background:#fff;border-radius:12px;padding:32px;box-shadow:0 4px 12px rgba(0,0,0,.08);}
              h2{color:#b91c1c;margin-top:0;}
              .info-box{background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:16px;margin:16px 0;}
              .row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #fee2e2;}
              .row:last-child{border-bottom:none;}
              .label{color:#6b7280;font-size:14px;}
              .value{font-weight:700;color:#111827;}
              .msg{background:#fff7ed;border:1px solid #fed7aa;border-radius:8px;padding:16px;margin:16px 0;color:#7c2d12;line-height:1.8;}
              .footer{font-size:12px;color:#9ca3af;margin-top:24px;text-align:center;}
            </style>
            </head>
            <body>
            <div class="wrap">
              <h2>⚠️ サービス停止のお知らせ</h2>
              <p>%s 様</p>
              <div class="msg">
                期日までにお支払いが確認できなかったので、サービスの提供を停止します。<br>
                既存のデータに関し、一切の保証は致しません。<br><br>
                サービスの再開をご希望の場合は、あらためて、運営事務局まで、お問合せください。
              </div>
              <div class="info-box">
                <div class="row"><span class="label">請求番号</span><span class="value">%s</span></div>
                <div class="row"><span class="label">プラン</span><span class="value">%s</span></div>
                <div class="row"><span class="label">未払い金額</span><span class="value">¥%s</span></div>
                <div class="row"><span class="label">お支払期限</span><span class="value">%s</span></div>
              </div>
              <div class="footer">ペットライフプラス | 自動送信メールのため返信はご遠慮ください</div>
            </div>
            </body></html>
            """.formatted(
                inv.ownerName(), inv.invoiceNumber(), inv.planName(),
                formatAmount(inv.amount()), inv.dueDate().format(DATE_FMT));
    }

    private String buildPaymentConfirmedHtml(InvoiceRow inv) {
        String validityRow = inv.subscriptionEndDate() != null
                ? "<div class=\"row\"><span class=\"label\">有効期限</span>"
                  + "<span class=\"value\" style=\"color:#16a34a;\">"
                  + inv.subscriptionEndDate().format(DATE_FMT) + " まで</span></div>"
                : "";

        return ("""
            <!DOCTYPE html>
            <html lang="ja">
            <head><meta charset="UTF-8">
            <style>
              body{font-family:'Noto Sans JP',sans-serif;background:#f8f9fa;margin:0;padding:20px;}
              .wrap{max-width:560px;margin:auto;background:#fff;border-radius:12px;padding:32px;box-shadow:0 4px 12px rgba(0,0,0,.08);}
              h2{color:#16a34a;margin-top:0;}
              .info-box{background:#f0fdf4;border-radius:8px;padding:16px;margin:16px 0;}
              .row{display:flex;justify-content:space-between;padding:6px 0;border-bottom:1px solid #e5e7eb;}
              .row:last-child{border-bottom:none;}
              .label{color:#6b7280;font-size:14px;}
              .value{font-weight:700;color:#111827;}
              .footer{font-size:12px;color:#9ca3af;margin-top:24px;text-align:center;}
            </style>
            </head>
            <body>
            <div class="wrap">
              <h2>✅ お支払い確認のご連絡</h2>
              <p>%s 様、契約料金のお支払いを確認しました。<br>
              引き続きペットライフプラスをご利用いただけます。</p>
              <div class="info-box">
                <div class="row"><span class="label">請求番号</span><span class="value">%s</span></div>
                <div class="row"><span class="label">プラン</span><span class="value">%s</span></div>
                <div class="row"><span class="label">お支払い金額</span><span class="value">¥%s</span></div>
                %s
              </div>
              <div style="text-align:center;margin:20px 0;">
                <a href="%s" style="display:inline-block;background:#16a34a;color:#fff;padding:10px 24px;border-radius:8px;text-decoration:none;font-weight:700;">🧾 領収書をダウンロードする</a>
              </div>
              <div class="footer">ペットライフプラス | 自動送信メールのため返信はご遠慮ください</div>
            </div>
            </body></html>
            """).formatted(
                inv.ownerName(), inv.invoiceNumber(), inv.planName(),
                formatAmount(inv.amount()), validityRow, invoiceUrl(inv));
    }
}
