package com.fashionsystem.fashion_system.filestorage;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;

final class ContentDispositions {
    private ContentDispositions() {
    }

    /** Tên file có dấu tiếng Việt → filename* (RFC 5987) kèm bản ASCII dự phòng. */
    static String of(boolean inline, String fileName) {
        ContentDisposition.Builder builder = inline ? ContentDisposition.inline() : ContentDisposition.attachment();
        return builder.filename(fileName == null || fileName.isBlank() ? "document" : fileName, StandardCharsets.UTF_8)
                .build().toString();
    }
}
