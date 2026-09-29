package com.coumba.dto.task;

import com.coumba.entities.task.Task;
import com.coumba.entities.user.User;
import com.coumba.entities.enums.TaskPriority;
import com.coumba.entities.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponseDTO {
    private Long id;
    private String title;
    private String description;
    private TaskStatus status;
    private TaskPriority priority;
    private LocalDate dueDate;
    private Double estimatedTime;
    private Double spentTime;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private Long projectId;
    private String projectName;
    private Long categoryId;
    private String categoryName;
    private Long creatorId;
    private String creatorName;
    private Set<Long> assigneeIds;

    public static TaskResponseDTO fromEntity(Task task) {
        return TaskResponseDTO.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .dueDate(task.getDueDate())
                .estimatedTime(task.getEstimatedTime())
                .spentTime(task.getSpentTime())
                .createdAt(task.getCreatedAt())
                .closedAt(task.getClosedAt())
                .projectId(task.getProject() != null ? task.getProject().getId() : null)
                .projectName(task.getProject() != null ? task.getProject().getName() : null)
                .categoryId(task.getCategory() != null ? task.getCategory().getId() : null)
                .categoryName(task.getCategory() != null ? task.getCategory().getName() : null)
                .creatorId(task.getCreator() != null ? task.getCreator().getId() : null)
                .creatorName(task.getCreator() != null ? task.getCreator().getFirstname() + " " + task.getCreator().getLastname() : null)
                .assigneeIds(task.getAssignees() != null ? task.getAssignees().stream().map(User::getId).collect(Collectors.toSet()) : Set.of())
                .build();
    }
}
