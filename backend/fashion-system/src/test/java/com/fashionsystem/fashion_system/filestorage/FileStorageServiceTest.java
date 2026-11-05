package com.fashionsystem.fashion_system.filestorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class FileStorageServiceTest {
    @TempDir
    Path dir;

    private static final byte[] PDF = "%PDF-1.7\nhello".getBytes(StandardCharsets.US_ASCII);

    private FileStorageService service(Clock clock) {
        DocumentStorageProperties props = new DocumentStorageProperties();
        props.getLocal().setDirectory(dir.toString());
        props.getLocal().setSigningKey("test-key");
        return new FileStorageService(new LocalDocumentStorage(props.getLocal(), clock), props);
    }

    @Test
    void storesOnlyMetadataFriendlyResultAndKeepsFileOutsideTheKeyFolder() {
        var stored = service(Clock.systemUTC()).store(new MockMultipartFile("file", "CV Nguyễn Văn A.pdf", "application/pdf", PDF),
                "recruitment/candidates/abc");
        assertThat(stored.storageKey()).startsWith("recruitment/candidates/abc/").endsWith(".pdf");
        assertThat(stored.fileName()).isEqualTo("CV Nguyễn Văn A.pdf");
        assertThat(stored.contentType()).isEqualTo("application/pdf");
        assertThat(stored.checksum()).hasSize(64);
        assertThat(Files.exists(dir.resolve(stored.storageKey()))).isTrue();
    }

    @Test
    void rejectsRenamedOrUnsupportedFiles() {
        FileStorageService service = service(Clock.systemUTC());
        assertThatThrownBy(() -> service.store(new MockMultipartFile("file", "virus.pdf", "application/pdf", "MZ...".getBytes()), "x"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("không khớp");
        assertThatThrownBy(() -> service.store(new MockMultipartFile("file", "a.exe", "application/octet-stream", PDF), "x"))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("PDF, DOC, DOCX, JPG, PNG");
        assertThat(FileStorageService.matchesSignature("docx", new byte[]{0x50, 0x4B, 0x03, 0x04, 0})).isTrue();
        assertThat(FileStorageService.matchesSignature("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})).isTrue();
        assertThat(FileStorageService.cleanFileName("..\\..\\etc/passwd")).isEqualTo("passwd");
    }

    @Test
    void signedLinksExpireAndCannotBeTamperedWith() {
        Instant start = Instant.parse("2026-11-04T00:00:00Z");
        DocumentStorageProperties props = new DocumentStorageProperties();
        props.getLocal().setDirectory(dir.toString());
        props.getLocal().setSigningKey("test-key");
        var early = new LocalDocumentStorage(props.getLocal(), Clock.fixed(start, ZoneOffset.UTC));
        early.put("a/b.pdf", new ByteArrayInputStream(PDF), PDF.length, "application/pdf");
        String url = early.signedUrl("a/b.pdf", Duration.ofMinutes(5), "cv.pdf", "application/pdf", true);
        String token = URLDecoder.decode(url.substring(url.indexOf("token=") + 6), StandardCharsets.UTF_8);

        assertThat(early.verify(token)).isNotNull().extracting(LocalDocumentStorage.Grant::inline).isEqualTo(true);
        var late = new LocalDocumentStorage(props.getLocal(), Clock.fixed(start.plusSeconds(301), ZoneOffset.UTC));
        assertThat(late.verify(token)).isNull();
        assertThat(early.verify(token.substring(0, token.length() - 2) + "xx")).isNull();
        assertThatThrownBy(() -> early.put("../../outside.pdf", new ByteArrayInputStream(PDF), PDF.length, "application/pdf"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
