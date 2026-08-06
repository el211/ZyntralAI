package com.zyntral.modules.auth.infrastructure;

import com.zyntral.modules.auth.application.EmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * SMTP adapter for {@link EmailSender}. Locally points at MailHog; in production at SES.
 * Links target the frontend, which then calls the API with the token.
 */
@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mail;
    private final String from;
    private final String webUrl;

    public SmtpEmailSender(JavaMailSender mail,
                           @Value("${zyntral.mail.from:no-reply@zyntral.ai}") String from,
                           @Value("${zyntral.web-url:http://localhost:3000}") String webUrl) {
        this.mail = mail;
        this.from = from;
        this.webUrl = webUrl;
    }

    @Override
    public void sendEmailVerification(String to, String name, String verifyToken) {
        send(to, "Verify your Zyntral AI account",
                "Hi %s,\n\nConfirm your email: %s/verify-email?token=%s\n\n— Zyntral AI"
                        .formatted(safe(name), webUrl, verifyToken));
    }

    @Override
    public void sendPasswordReset(String to, String name, String resetToken) {
        send(to, "Reset your Zyntral AI password",
                "Hi %s,\n\nReset your password: %s/reset-password?token=%s\n\nIf you didn't request this, ignore this email.\n\n— Zyntral AI"
                        .formatted(safe(name), webUrl, resetToken));
    }

    @Override
    public void sendWorkspaceInvitation(String to, String workspaceName, String inviteToken) {
        send(to, "You've been invited to a Zyntral AI workspace",
                "You've been invited to join \"%s\".\n\nAccept: %s/invitations/accept?token=%s\n\n— Zyntral AI"
                        .formatted(workspaceName, webUrl, inviteToken));
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mail.send(msg);
        } catch (Exception ex) {
            // never let a mail failure break the request; log for retry/alerting
            log.error("Failed to send '{}' email to {}", subject, to, ex);
        }
    }

    private String safe(String name) {
        return name == null || name.isBlank() ? "there" : name;
    }
}
