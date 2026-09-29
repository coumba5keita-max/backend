package com.coumba.dto.task;

import com.coumba.entities.task.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponseDTO {

    private Long id;
    private Long taskId;
    private String content;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private LocalDateTime createdAt;

    public static CommentResponseDTO fromEntity(Comment comment) {
        if (comment == null) return null;

        Long aId = comment.getUser() != null ? comment.getUser().getId() : null;
        String aName = comment.getUser() != null ? comment.getUser().getFirstname() + " " + comment.getUser().getLastname() : null;
        String aEmail = comment.getUser() != null ? comment.getUser().getEmail() : null;
        Long tId = comment.getTask() != null ? comment.getTask().getId() : null;

        return CommentResponseDTO.builder()
                .id(comment.getId())
                .taskId(tId)
                .content(comment.getContent())
                .authorId(aId)
                .authorName(aName)
                .authorEmail(aEmail)
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
