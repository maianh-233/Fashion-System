package com.fashionsystem.fashion_system.helper;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/** Helper gửi email text, HTML, OTP và email có file đính kèm. */
@Component
@RequiredArgsConstructor
public class MailHelper {
    private final JavaMailSender mailSender;

    @Value("${mail.from-address}")
    private String fromAddress;

    @Value("${mail.from-name}")
    private String fromName;

    @Value("${mail.otp-subject}")
    private String otpSubject;

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

    private void validateRequired(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " khong duoc de trong");
        }
    }
}
