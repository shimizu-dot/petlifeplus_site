package com.example.petlife.service;

import com.example.petlife.dto.contact.ContactRequest;
import com.example.petlife.exception.BadRequestException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class ContactService {

    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    private final JavaMailSender mailSender;

    @Value("${sendgrid.api-key:}")
    private String sendgridApiKey;

    @Value("${sendgrid.from-email:noreply@petlife.local}")
    private String fromEmail;

    @Value("${sendgrid.from-name:ペットライフプラス}")
    private String fromName;

    @Value("${contact.to-email:}")
    private String contactToEmail;

    public ContactService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(ContactRequest request) {
        if (sendgridApiKey == null || sendgridApiKey.isBlank()) {
            log.warn("SENDGRID_API_KEY not configured. Contact mail not sent from={}", request.email());
            throw new BadRequestException("お問い合わせ送信設定が未完了です。しばらくしてから再度お試しください。");
        }

        String to = (contactToEmail == null || contactToEmail.isBlank()) ? fromEmail : contactToEmail;
        if (to == null || to.isBlank()) {
            throw new BadRequestException("お問い合わせ送信先が設定されていません。");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setReplyTo(request.email(), request.name());
            helper.setSubject("【ペットライフプラス】お問い合わせ: " + request.name());
            helper.setText(buildHtml(request), true);
            mailSender.send(message);
            log.info("Contact mail sent from={} to={}", request.email(), to);
        } catch (MessagingException | java.io.UnsupportedEncodingException | MailException e) {
            log.error("Failed to send contact mail from={}", request.email(), e);
            throw new BadRequestException("お問い合わせの送信に失敗しました。時間をおいて再度お試しください。");
        }
    }

    private String buildHtml(ContactRequest request) {
        String name = HtmlUtils.htmlEscape(request.name());
        String email = HtmlUtils.htmlEscape(request.email());
        String body = HtmlUtils.htmlEscape(request.message()).replace("\n", "<br>");
        return """
                <!DOCTYPE html>
                <html lang="ja">
                <body style="font-family:'Noto Sans JP',sans-serif;background:#f8fafc;margin:0;padding:24px;">
                  <div style="max-width:640px;margin:0 auto;background:#fff;border-radius:12px;padding:28px;border:1px solid #e5e7eb;">
                    <h1 style="font-size:20px;margin:0 0 18px;color:#1f2937;">お問い合わせを受信しました</h1>
                    <p style="margin:0 0 10px;color:#374151;"><strong>お名前:</strong> %s</p>
                    <p style="margin:0 0 18px;color:#374151;"><strong>メールアドレス:</strong> %s</p>
                    <div style="padding:16px;background:#f8fafc;border-radius:10px;color:#374151;line-height:1.7;">%s</div>
                  </div>
                </body>
                </html>
                """.formatted(name, email, body);
    }
}
