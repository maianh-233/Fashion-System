package com.fashionsystem.fashion_system.helper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import com.fashionsystem.fashion_system.helper.mail.MailAttachment;
import com.fashionsystem.fashion_system.helper.mail.MailRequest;
import com.fashionsystem.fashion_system.helper.mail.MailSendException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/** Helper gửi email text, HTML, OTP và email có file đính kèm. */
@Component
public class MailHelper {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${mail.from-address}")
    private String fromAddress;

    @Value("${mail.from-name}")
    private String fromName;

    @Value("${mail.otp-subject}")
    private String otpSubject;

    public MailHelper(JavaMailSender mailSender, ObjectProvider<TemplateEngine> templateEngineProvider) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngineProvider.getIfAvailable();
    }

    public void sendText(String to, String subject, String content) {
        send(to, subject, content, false, List.of());
    }

    public void sendHtml(String to, String subject, String html) {
        send(to, subject, html, true, List.of());
    }

    /** Gửi HTML cùng danh sách file; chỉ chấp nhận đường dẫn tới file thường đang tồn tại. */
    public void sendWithAttachments(String to, String subject, String html, List<Path> attachments) {
        send(to, subject, html, true, attachments == null ? List.of() : List.copyOf(attachments));
    }

    public void sendTextMail(MailRequest request) {
        sendRequest(request, false);
    }

    public void sendHtmlMail(MailRequest request) {
        sendRequest(request, true);
    }

    public void sendTemplateMail(MailRequest request, String templateName, Map<String, Object> variables) {
        if (templateEngine == null) throw new MailSendException("Mail template engine is unavailable", null);
        validateRequired(templateName, "Template name");
        Context context = new Context();
        context.setVariables(variables == null ? Map.of() : Map.copyOf(variables));
        String html;
        try {
            html = templateEngine.process(normalizeTemplateName(templateName), context);
        } catch (RuntimeException exception) {
            throw new MailSendException("Mail template rendering failed", exception);
        }
        MailRequest rendered = new MailRequest(
                request.to(), request.cc(), request.bcc(), request.subject(), request.textContent(), html,
                request.replyTo(), request.attachments());
        sendHtmlMail(rendered);
    }

    public void sendMailWithAttachment(MailRequest request, MailAttachment attachment) {
        sendMailWithMultipleAttachments(request, List.of(attachment));
    }

    public void sendMailWithMultipleAttachments(MailRequest request, List<MailAttachment> attachments) {
        MailRequest withAttachments = new MailRequest(
                request.to(), request.cc(), request.bcc(), request.subject(), request.textContent(),
                request.htmlContent(), request.replyTo(), attachments);
        sendRequest(withAttachments, StringUtils.hasText(request.htmlContent()));
    }

    public void sendOtp(String to, String recipientName, String otp, long expirationMinutes) {
        String safeName = HtmlUtils.htmlEscape(
                StringUtils.hasText(recipientName) ? recipientName.trim() : "ban");
        String safeOtp = HtmlUtils.htmlEscape(otp);
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:560px;margin:auto;color:#222">
                  <h2>Dat lai mat khau</h2>
                  <p>Xin chao %s,</p>
                  <p>Ma OTP cua ban la:</p>
                  <p style="font-size:30px;font-weight:700;letter-spacing:8px">%s</p>
                  <p>Ma co hieu luc trong %d phut va chi duoc su dung mot lan.</p>
                  <p>Neu ban khong yeu cau dat lai mat khau, hay bo qua email nay.</p>
                </div>
                """.formatted(safeName, safeOtp, expirationMinutes);
        sendHtml(to, otpSubject, html);
    }

    private void send(String to, String subject, String content, boolean html, List<Path> attachments) {
        validateRequired(to, "Email nguoi nhan");
        validateRequired(subject, "Tieu de email");
        validateRequired(content, "Noi dung email");
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, !attachments.isEmpty(), StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(content, html);
            for (Path attachment : attachments) {
                Path normalized = attachment.toAbsolutePath().normalize();
                if (!Files.isRegularFile(normalized)) {
                    throw new IllegalArgumentException("File dinh kem khong ton tai: " + normalized);
                }
                helper.addAttachment(normalized.getFileName().toString(), new FileSystemResource(normalized));
            }
            mailSender.send(message);
        } catch (MessagingException | IOException exception) {
            throw new MailSendException("Khong the gui email", exception);
        }
    }

    private void sendRequest(MailRequest request, boolean html) {
        if (request == null) throw new IllegalArgumentException("Mail request is required");
        validateRecipients(request.to(), "to");
        validateRequired(request.subject(), "Subject");
        String content = html ? request.htmlContent() : request.textContent();
        validateRequired(content, html ? "HTML content" : "Text content");
        validateAttachments(request.attachments());
        try {
            boolean multipart = !request.attachments().isEmpty();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, StandardCharsets.UTF_8.name());
            helper.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(request.to().toArray(String[]::new));
            if (!request.cc().isEmpty()) helper.setCc(request.cc().toArray(String[]::new));
            if (!request.bcc().isEmpty()) helper.setBcc(request.bcc().toArray(String[]::new));
            if (StringUtils.hasText(request.replyTo())) helper.setReplyTo(request.replyTo().trim());
            helper.setSubject(request.subject().trim());
            helper.setText(content, html);
            for (MailAttachment attachment : request.attachments()) {
                ByteArrayResource resource = new ByteArrayResource(attachment.content()) {
                    @Override public String getFilename() { return attachment.fileName(); }
                };
                helper.addAttachment(attachment.fileName(), resource, attachment.contentType());
            }
            mailSender.send(message);
        } catch (MessagingException | IOException | org.springframework.mail.MailException exception) {
            throw new MailSendException("Mail delivery failed", exception);
        }
    }

    private void validateRecipients(List<String> recipients, String field) {
        if (recipients == null || recipients.isEmpty()) {
            throw new IllegalArgumentException("At least one " + field + " recipient is required");
        }
        recipients.forEach(value -> validateRequired(value, field + " recipient"));
    }

    private void validateAttachments(List<MailAttachment> attachments) {
        for (MailAttachment attachment : attachments) {
            if (attachment == null) throw new IllegalArgumentException("Attachment is required");
            validateRequired(attachment.fileName(), "Attachment file name");
            validateRequired(attachment.contentType(), "Attachment content type");
            if (attachment.content() == null || attachment.content().length == 0) {
                throw new IllegalArgumentException("Attachment content must not be empty");
            }
        }
    }

    private String normalizeTemplateName(String templateName) {
        String normalized = templateName.trim().replace('\\', '/');
        if (normalized.contains("..") || normalized.startsWith("/") || !normalized.matches("[A-Za-z0-9_/-]+")) {
            throw new IllegalArgumentException("Invalid mail template name");
        }
        return normalized.startsWith("mail/") ? normalized : "mail/" + normalized;
    }

    private void validateRequired(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " khong duoc de trong");
        }
    }
}
