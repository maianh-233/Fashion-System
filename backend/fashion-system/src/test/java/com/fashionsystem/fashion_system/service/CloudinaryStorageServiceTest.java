package com.fashionsystem.fashion_system.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.fashionsystem.fashion_system.config.StorageProperties;
import com.fashionsystem.fashion_system.dto.StorageUploadResult;
import com.fashionsystem.fashion_system.exception.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

/** Catches unsafe reliance on multipart names/MIME and malformed Cloudinary responses. */
class CloudinaryStorageServiceTest {
    private Cloudinary cloudinary;
    private Uploader uploader;
    private CloudinaryStorageService service;

    @BeforeEach
    void setUp() {
        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        StorageProperties properties = new StorageProperties();
        properties.setMaxFileSize(DataSize.ofKilobytes(10));
        service = new CloudinaryStorageService(cloudinary, properties);
    }

    @Test
    void rejectsSpoofedPngBeforeCallingCloudinary() {
        var file = new MockMultipartFile(
                "file", "photo.png", "image/png", "not-a-png".getBytes(UTF_8));

        assertThatThrownBy(() -> service.uploadImage(file, "products/variants"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nội dung");
        verifyNoInteractions(cloudinary, uploader);
    }

    @Test
    void rejectsOversizedImageBeforeCallingCloudinary() {
        var file = new MockMultipartFile(
                "file", "large.png", "image/png", new byte[10 * 1024 + 1]);

        assertThatThrownBy(() -> service.uploadImage(file, "products/variants"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dung lượng");
        verifyNoInteractions(cloudinary, uploader);
    }

    @Test
    void returnsOnlyValidatedSecureUrlAndPublicIdFromCloudinary() throws Exception {
        byte[] png = new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1};
        var file = new MockMultipartFile("file", "ignored.png", "image/png", png);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/product.png",
                "public_id", "fashion-system/products/variants/generated"));

        StorageUploadResult result = service.uploadImage(file, "products/variants");

        assertThat(result.secureUrl())
                .isEqualTo("https://res.cloudinary.com/demo/image/upload/product.png");
        assertThat(result.publicId())
                .isEqualTo("fashion-system/products/variants/generated");
    }

    @Test
    void rejectsCloudinaryResponseWithoutHttpsUrl() throws Exception {
        byte[] jpeg = new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1};
        var file = new MockMultipartFile("file", "ignored.jpg", "image/jpeg", jpeg);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "http://example.test/product.jpg",
                "public_id", "fashion-system/products/generated"));

        assertThatThrownBy(() -> service.uploadImage(file, "products"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("không hợp lệ");
    }
}
