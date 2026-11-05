package com.fashionsystem.fashion_system.filestorage;

import com.fashionsystem.fashion_system.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Dịch vụ lưu tài liệu dùng chung cho nghiệp vụ: kiểm tra loại file (đuôi + chữ ký nội dung), giới hạn dung lượng,
 * đặt storage key, tính SHA-256 và cấp link đã ký. Nghiệp vụ chỉ lưu metadata + storage key trong database, không lưu
 * nội dung file. Nhà cung cấp cụ thể nằm sau {@link DocumentStorage}.
 */
@Slf4j
@Service
public class FileStorageService {
    /** Đuôi file được nhận → content type chuẩn. */
    static final Map<String, String> ALLOWED = Map.of(
            "pdf", "application/pdf",
            "doc", "application/msword",
            "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "png", "image/png");

    private final DocumentStorage storage;
    private final DocumentStorageProperties properties;

    public FileStorageService(DocumentStorage storage, DocumentStorageProperties properties) {
        this.storage = storage;
        this.properties = properties;
    }

    public record StoredDocument(String provider, String storageKey, String fileName, String contentType, long size,
                                 String checksum) {
    }

    public record SignedLink(String url, long expiresInSeconds) {
    }

    /**
     * Kiểm tra và tải file lên kho private.
     *
     * @param folder thư mục logic, ví dụ {@code recruitment/candidates/<id>}
     */
    public StoredDocument store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) throw BusinessException.badRequest("Vui lòng chọn file");
        long max = properties.getMaxFileSize().toBytes();
        if (file.getSize() > max)
            throw BusinessException.badRequest("File vượt quá " + properties.getMaxFileSize().toMegabytes() + "MB");
        String fileName = cleanFileName(file.getOriginalFilename());
        String extension = extension(fileName);
        String contentType = ALLOWED.get(extension);
        if (contentType == null) throw BusinessException.badRequest("Chỉ nhận file PDF, DOC, DOCX, JPG, PNG");
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw BusinessException.badRequest("Không đọc được file tải lên");
        }
        if (!matchesSignature(extension, bytes))
            throw BusinessException.badRequest("Nội dung file không khớp định dạng ." + extension);
        String key = normalizeFolder(folder) + "/" + UUID.randomUUID() + "." + (extension.equals("jpeg") ? "jpg" : extension);
        try {
            storage.put(key, new ByteArrayInputStream(bytes), bytes.length, contentType);
        } catch (RuntimeException e) {
            log.error("Document upload failed for key {}", key, e);
            throw BusinessException.serviceUnavailable("Không lưu được file, vui lòng thử lại", e);
        }
        return new StoredDocument(storage.provider(), key, fileName, contentType, bytes.length, sha256(bytes));
    }

    /** Link đã ký có hạn để xem trên trình duyệt ({@code inline}) hoặc tải xuống. */
    public SignedLink signedUrl(String storageKey, String fileName, String contentType, boolean inline) {
        Duration ttl = properties.getSignedUrlTtl();
        return new SignedLink(storage.signedUrl(storageKey, ttl, fileName, contentType, inline), ttl.toSeconds());
    }

    /** Xóa object; lỗi chỉ ghi log để không chặn nghiệp vụ (metadata đã đánh dấu xóa). */
    public void deleteQuietly(String storageKey) {
        try {
            storage.delete(storageKey);
        } catch (RuntimeException e) {
            log.warn("Could not delete document {}", storageKey, e);
        }
    }

    public String provider() {
        return storage.provider();
    }

    static String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Chữ ký đầu file: chặn file đổi đuôi (ví dụ .exe đổi thành .pdf). */
    static boolean matchesSignature(String extension, byte[] bytes) {
        return switch (extension) {
            case "pdf" -> startsWith(bytes, 0x25, 0x50, 0x44, 0x46); // %PDF
            case "docx" -> startsWith(bytes, 0x50, 0x4B, 0x03, 0x04); // ZIP (OOXML)
            case "doc" -> startsWith(bytes, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1); // OLE2
            case "jpg", "jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            default -> false;
        };
    }

    static String cleanFileName(String original) {
        String name = StringUtils.getFilename(original == null ? "" : original.replace('\\', '/'));
        if (name == null) name = "";
        name = name.replaceAll("[\\p{Cntrl}\"<>|:*?]", "").trim();
        if (name.length() > 200) {
            String ext = extension(name);
            name = name.substring(0, 200 - ext.length() - 1) + "." + ext;
        }
        return name.isEmpty() ? "document" : name;
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) if ((bytes[i] & 0xFF) != signature[i]) return false;
        return true;
    }

    private static String normalizeFolder(String folder) {
        String value = folder == null ? "" : folder.trim().replace('\\', '/');
        value = value.replaceAll("[^A-Za-z0-9/_-]", "").replaceAll("/{2,}", "/").replaceAll("^/|/$", "");
        if (value.contains("..") || value.isEmpty()) return "documents";
        return value;
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
