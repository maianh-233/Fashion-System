package com.fashionsystem.fashion_system.filestorage;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phục vụ link đã ký của kho local (chỉ khi {@code document-storage.provider=local}). Không cần JWT: token HMAC có hạn
 * chính là quyền đọc, giống presigned URL của R2/S3.
 */
@RestController
public class LocalFileController {
    private final DocumentStorage storage;

    public LocalFileController(DocumentStorage storage) {
        this.storage = storage;
    }

    @GetMapping(LocalDocumentStorage.DOWNLOAD_PATH)
    public ResponseEntity<Resource> download(@RequestParam String token) {
        if (!(storage instanceof LocalDocumentStorage local)) return ResponseEntity.notFound().build();
        LocalDocumentStorage.Grant grant = local.verify(token);
        if (grant == null) return ResponseEntity.status(403).build();
        MediaType type;
        try {
            type = grant.contentType().isBlank() ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(grant.contentType());
        } catch (IllegalArgumentException e) {
            type = MediaType.APPLICATION_OCTET_STREAM;
        }
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDispositions.of(grant.inline(), grant.fileName()))
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(grant.file()));
    }
}
