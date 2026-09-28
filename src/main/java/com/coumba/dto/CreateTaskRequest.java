package com.coumba.dto;

import com.coumba.entities.enums.TaskPriority;
import com.coumba.entities.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTaskRequest {

    @NotBlank(message = "Le titre est obligatoire")
    private String title;

    private String description;

    @Builder.Default
    private TaskStatus status = TaskStatus.A_FAIRE;

    @Builder.Default
    private TaskPriority priority = TaskPriority.MOYENNE;

    private LocalDate dueDate;

    private Double estimatedTime;

    private Double spentTime;

    @NotNull(message = "L'ID du projet est obligatoire")
    private Long projectId;

    private Long categoryId;

    private Set<Long> assigneeIds;
}
