package com.fashionsystem.fashion_system.service;

import com.fashionsystem.fashion_system.dto.StorageUploadResult;
import org.springframework.web.multipart.MultipartFile;

/** Reusable boundary for validated image storage. */
public interface StorageService {
    /** Uploads a validated image into a server-owned folder. */
    StorageUploadResult uploadImage(MultipartFile file, String folder);

    /** Deletes an image using a public id previously returned by the provider. */
    void deleteImage(String publicId);
}
