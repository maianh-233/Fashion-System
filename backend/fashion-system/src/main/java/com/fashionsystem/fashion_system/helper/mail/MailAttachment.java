package com.fashionsystem.fashion_system.helper.mail;

import java.util.Arrays;

/** Provider-neutral in-memory mail attachment. */
public record MailAttachment(String fileName, byte[] content, String contentType) {
    public MailAttachment {
        content = content == null ? null : Arrays.copyOf(content, content.length);
    }

    @Override
    public byte[] content() {
        return content == null ? null : Arrays.copyOf(content, content.length);
    }
}
