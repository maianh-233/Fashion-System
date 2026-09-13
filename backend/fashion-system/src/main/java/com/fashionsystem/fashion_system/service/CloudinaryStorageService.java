package com.fashionsystem.fashion_system.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.fashionsystem.fashion_system.config.StorageProperties;
import com.fashionsystem.fashion_system.dto.StorageUploadResult;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Cloudinary adapter that validates image content before calling the external provider. */
@Service
@RequiredArgsConstructor
public class CloudinaryStorageService implements StorageService {
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    private final Cloudinary cloudinary;
    private final StorageProperties properties;

    /**
     * Validates size, declared MIME and magic bytes, then uploads under a generated public id.
     * Client filenames and identifiers are never used as storage ownership data.
     */
    @Override
    public StorageUploadResult uploadImage(MultipartFile file, String folder) {
        byte[] bytes = validateAndRead(file);
        String safeFolder = normalizeFolder(folder);
        try {
            Map<?, ?> response = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "resource_type", "image",
                    "folder", "fashion-system/" + safeFolder,
                    "public_id", UUID.randomUUID().toString(),
                    "overwrite", false,
                    "unique_filename", false));
            String secureUrl = stringValue(response.get("secure_url"));
            String publicId = stringValue(response.get("public_id"));
            if (!isSecureUrl(secureUrl) || publicId == null) {
                throw BusinessException.serviceUnavailable("Phản hồi lưu trữ hình ảnh không hợp lệ");
            }
            return new StorageUploadResult(secureUrl, publicId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.serviceUnavailable("Không thể lưu hình ảnh lúc này", exception);
        }
    }

    /** Deletes a provider asset without exposing provider error details to API callers. */
    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw BusinessException.badRequest("Mã hình ảnh lưu trữ không hợp lệ");
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
        } catch (IOException | RuntimeException exception) {
            throw BusinessException.serviceUnavailable("Không thể xóa hình ảnh lúc này", exception);
        }
    }

    private byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("Tệp hình ảnh không được để trống");
        }
        long maxBytes = properties.getMaxFileSize().toBytes();
        if (file.getSize() > maxBytes) {
            throw BusinessException.badRequest("Tệp hình ảnh vượt quá dung lượng cho phép");
        }
        String contentType = file.getContentType() == null
                ? "" : file.getContentType().trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw BusinessException.badRequest("Định dạng hình ảnh không được hỗ trợ");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > maxBytes) {
                throw BusinessException.badRequest("Tệp hình ảnh vượt quá dung lượng cho phép");
            }
            if (!matchesSignature(contentType, bytes)) {
                throw BusinessException.badRequest("Tệp có nội dung hình ảnh không hợp lệ");
            }
            return bytes;
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw BusinessException.badRequest("Không thể đọc tệp hình ảnh");
        }
    }

    private boolean matchesSignature(String contentType, byte[] bytes) {
        return switch (contentType) {
            case "image/jpeg" -> bytes.length >= 3
                    && unsigned(bytes[0]) == 0xff && unsigned(bytes[1]) == 0xd8 && unsigned(bytes[2]) == 0xff;
            case "image/png" -> bytes.length >= 8
                    && unsigned(bytes[0]) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e
                    && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a
                    && bytes[6] == 0x1a && bytes[7] == 0x0a;
            case "image/gif" -> bytes.length >= 6
                    && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                    && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
            case "image/webp" -> bytes.length >= 12
                    && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
            default -> false;
        };
    }

    private int unsigned(byte value) {
        return value & 0xff;
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank() || !folder.matches("[A-Za-z0-9/_-]+")) {
            throw BusinessException.badRequest("Thư mục lưu trữ hình ảnh không hợp lệ");
        }
        return folder.replaceAll("^/+|/+$", "");
    }

    private String stringValue(Object value) {
        if (!(value instanceof String text) || text.isBlank()) {
            return null;
        }
        return text.trim();
    }

    private boolean isSecureUrl(String value) {
        if (value == null) return false;
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
