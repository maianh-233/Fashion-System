package com.fashionsystem.fashion_system.dto;

/** Trusted identifiers returned by the configured cloud storage provider. */
public record StorageUploadResult(String secureUrl, String publicId) {
}
