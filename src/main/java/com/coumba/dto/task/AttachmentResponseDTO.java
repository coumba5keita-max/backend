package com.coumba.dto.task;

import com.coumba.entities.task.Attachment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentResponseDTO {

    private Long id;
    private Long taskId;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String downloadUrl;
    private Long uploadedById;
    private String uploadedByName;
    private LocalDateTime uploadedAt;

    public static AttachmentResponseDTO fromEntity(Attachment attachment) {
        if (attachment == null) return null;

        Long uId = attachment.getUser() != null ? attachment.getUser().getId() : null;
        String uName = attachment.getUser() != null ? attachment.getUser().getFirstname() + " " + attachment.getUser().getLastname() : null;
        Long tId = attachment.getTask() != null ? attachment.getTask().getId() : null;

        return AttachmentResponseDTO.builder()
                .id(attachment.getId())
                .taskId(tId)
                .fileName(attachment.getFileName())
                .fileType(attachment.getFileType())
                .fileSize(attachment.getFileSize())
                .downloadUrl("/api/attachments/" + attachment.getId() + "/download")
                .uploadedById(uId)
                .uploadedByName(uName)
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }
}
