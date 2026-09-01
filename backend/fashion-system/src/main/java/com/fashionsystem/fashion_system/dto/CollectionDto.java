package com.fashionsystem.fashion_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng để truyền dữ liệu của Collection giữa các tầng ứng dụng mà không làm lộ trực tiếp entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionDto {
    private UUID id;
    private UUID brandId;
    @NotBlank
    @Size(max = 255)
    private String name;
    @Size(max = 100)
    private String code;
    @Size(max = 50)
    private String season;
    private Integer year;
    private LocalDate releaseDate;
    private String description;
    private String imageUrl;
    @Size(max = 50)
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
