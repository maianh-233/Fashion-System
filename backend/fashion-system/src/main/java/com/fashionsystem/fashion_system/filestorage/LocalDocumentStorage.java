package com.fashionsystem.fashion_system.filestorage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Kho tài liệu trên đĩa cho môi trường phát triển (chưa có khóa R2/S3). Mô phỏng đúng cơ chế link đã ký: file nằm
 * ngoài thư mục web, chỉ đọc được qua {@code /api/files/local?token=...} với token HMAC có hạn.
 */
public class LocalDocumentStorage implements DocumentStorage {
    static final String DOWNLOAD_PATH = "/api/files/local";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final Path root;
    private final byte[] signingKey;
    private final Clock clock;

    public LocalDocumentStorage(DocumentStorageProperties.Local config, Clock clock) {
        this.root = Path.of(config.getDirectory()).toAbsolutePath().normalize();
        if (config.getSigningKey().isBlank()) {
            this.signingKey = new byte[32];
            new SecureRandom().nextBytes(this.signingKey);
        } else {
            this.signingKey = config.getSigningKey().getBytes(StandardCharsets.UTF_8);
        }
        this.clock = clock;
    }

    @Override
    public String provider() {
        return "local";
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String signedUrl(String key, Duration ttl, String fileName, String contentType, boolean inline) {
        long expires = clock.instant().plus(ttl).getEpochSecond();
        String payload = String.join("\n", key, Long.toString(expires), inline ? "i" : "a",
                fileName == null ? "" : fileName, contentType == null ? "" : contentType);
        String encoded = B64.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return DOWNLOAD_PATH + "?token=" + URLEncoder.encode(encoded + "." + sign(encoded), StandardCharsets.UTF_8);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Token hợp lệ và còn hạn → thông tin file; ngược lại null. */
    Grant verify(String token) {
        if (token == null) return null;
        int dot = token.lastIndexOf('.');
        if (dot <= 0) return null;
        String encoded = token.substring(0, dot);
        if (!MessageDigest.isEqual(sign(encoded).getBytes(StandardCharsets.US_ASCII),
                token.substring(dot + 1).getBytes(StandardCharsets.US_ASCII))) return null;
        String[] parts;
        try {
            parts = new String(B64D.decode(encoded), StandardCharsets.UTF_8).split("\n", -1);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (parts.length != 5) return null;
        long expires;
        try {
            expires = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return null;
        }
        if (clock.instant().getEpochSecond() > expires) return null;
        Path file;
        try {
            file = resolve(parts[0]);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return Files.isRegularFile(file) ? new Grant(file, "i".equals(parts[2]), parts[3], parts[4]) : null;
    }

    record Grant(Path file, boolean inline, String fileName, String contentType) {
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("Storage key không hợp lệ");
        return path;
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return B64.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
