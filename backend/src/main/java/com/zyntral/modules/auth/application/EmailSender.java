package com.zyntral.modules.auth.application;

/**
 * Outbound email port. The default implementation uses SMTP (JavaMailSender); swapping
 * to SES/SendGrid is a new adapter, no caller changes.
 */
public interface EmailSender {

    void sendEmailVerification(String to, String name, String verifyToken);

    void sendPasswordReset(String to, String name, String resetToken);

    void sendWorkspaceInvitation(String to, String workspaceName, String inviteToken);
}
