package com.fashionsystem.fashion_system.helper.mail;

import java.util.List;

/** Generic message envelope independent of any business module. */
public record MailRequest(
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String textContent,
        String htmlContent,
        String replyTo,
        List<MailAttachment> attachments) {

    public MailRequest {
        to = immutable(to);
        cc = immutable(cc);
        bcc = immutable(bcc);
        attachments = immutable(attachments);
    }

    public MailRequest(List<String> to, String subject, String textContent, String htmlContent) {
        this(to, List.of(), List.of(), subject, textContent, htmlContent, null, List.of());
    }

    private static <T> List<T> immutable(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
