package com.example.petlife.service;

import com.example.petlife.entity.PasswordResetTokenEntity;
import com.example.petlife.entity.UserEntity;
import com.example.petlife.mapper.PasswordResetTokenMapper;
import com.example.petlife.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int EXPIRE_MINUTES = 30;

    private final UserMapper userMapper;
    private final PasswordResetTokenMapper tokenMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${sendgrid.api-key:}")
    private String sendgridApiKey;

    @Value("${sendgrid.from-email:noreply@petlifeplus.local}")
    private String fromEmail;

    @Value("${sendgrid.from-name:ペットライフプラス}")
    private String fromName;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public PasswordResetService(UserMapper userMapper,
                                PasswordResetTokenMapper tokenMapper,
                                BCryptPasswordEncoder passwordEncoder,
                                JavaMailSender mailSender) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
    }

    /**
     * メールアドレスに対してパスワードリセットメールを送信する。
     * メールアドレスが存在しない場合でも成功扱いにする（ユーザー列挙攻撃の防止）。
     */
    public void initiateReset(String email) {
        UserEntity user = userMapper.findByEmail(email.trim().toLowerCase());
        if (user == null) {
            log.info("Password reset requested for unknown email: {}", email);
            return;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES);
        tokenMapper.insert(new PasswordResetTokenEntity(null, user.id(), token, expiresAt, null, null));

        sendResetEmail(user, token);
    }

    /** トークンが有効（存在・未使用・未期限切れ）かどうかを確認する */
    public boolean isValidToken(String token) {
        PasswordResetTokenEntity entity = tokenMapper.findByToken(token);
        return entity != null
                && entity.usedAt() == null
                && entity.expiresAt().isAfter(LocalDateTime.now());
    }

    /**
     * トークンを使ってパスワードをリセットする。
     * 成功時は true を返す。トークンが無効な場合は false。
     */
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetTokenEntity entity = tokenMapper.findByToken(token);
        if (entity == null || entity.usedAt() != null || entity.expiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        userMapper.updatePasswordById(entity.userId(), passwordEncoder.encode(newPassword));
        tokenMapper.markUsed(entity.id());
        return true;
    }

    private void sendResetEmail(UserEntity user, String token) {
        if (sendgridApiKey == null || sendgridApiKey.isBlank()) {
            log.warn("SENDGRID_API_KEY not configured. Password reset email not sent for user id={}. " +
                     "Reset URL: {}/app/reset-password?token={}", user.id(), baseUrl, token);
            return;
        }

        String resetUrl = baseUrl + "/app/reset-password?token=" + token;
        String subject = "【ペットライフプラス】パスワード再設定のご案内";
        String html = buildEmailHtml(user.name(), resetUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(user.email());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Password reset email sent to user id={}", user.id());
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send password reset email to user id={}", user.id(), e);
        }
    }

    private String buildEmailHtml(String name, String resetUrl) {
        return """
                <!DOCTYPE html>
                <html lang="ja">
                <body style="font-family:'Noto Sans JP',sans-serif;background:#f8fafc;margin:0;padding:32px;">
                  <div style="max-width:520px;margin:0 auto;background:#fff;border-radius:14px;padding:40px;box-shadow:0 2px 8px rgba(0,0,0,.08);">
                    <div style="font-size:22px;font-weight:700;color:#1D4ED8;margin-bottom:24px;">ペットライフプラス</div>
                    <p style="color:#374151;">%s 様</p>
                    <p style="color:#374151;">パスワード再設定のご依頼を受け付けました。<br>
                    下記のボタンから新しいパスワードを設定してください。</p>
                    <div style="text-align:center;margin:32px 0;">
                      <a href="%s"
                         style="display:inline-block;background:#1D4ED8;color:#fff;text-decoration:none;
                                padding:14px 32px;border-radius:14px;font-weight:700;font-size:15px;">
                        パスワードを再設定する
                      </a>
                    </div>
                    <p style="color:#6b7280;font-size:13px;">
                      このリンクの有効期限は <strong>%d 分</strong> です。<br>
                      心当たりのない場合はこのメールを無視してください。
                    </p>
                    <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;">
                    <p style="color:#9ca3af;font-size:12px;">ペットライフプラス運営事務局</p>
                  </div>
                </body>
                </html>
                """.formatted(name, resetUrl, EXPIRE_MINUTES);
    }
}
