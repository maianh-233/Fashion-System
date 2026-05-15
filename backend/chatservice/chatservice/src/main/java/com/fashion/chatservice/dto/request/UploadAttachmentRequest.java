package com.fashion.chatservice.dto.request;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadAttachmentRequest {

    private String fileName;

    private String fileUrl;

    private String fileType;

    private Long fileSize;
}
