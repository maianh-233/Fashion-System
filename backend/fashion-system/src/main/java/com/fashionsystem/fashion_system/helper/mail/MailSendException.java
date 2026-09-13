package com.fashionsystem.fashion_system.helper.mail;

import org.springframework.mail.MailException;

/** Stable mail infrastructure exception that remains compatible with Spring Mail callers. */
public class MailSendException extends MailException {
    public MailSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
