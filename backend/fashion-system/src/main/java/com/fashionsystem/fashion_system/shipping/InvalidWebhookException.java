package com.fashionsystem.fashion_system.shipping;

/** Webhook không chứng minh được đến từ hãng (sai chữ ký / khóa API). */
public class InvalidWebhookException extends RuntimeException {
    public InvalidWebhookException(String message) {
        super(message);
    }
}
