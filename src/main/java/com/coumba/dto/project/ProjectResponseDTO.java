package com.coumba.dto.project;

import com.coumba.entities.project.Project;
import com.coumba.entities.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Boolean isArchived;
    private LocalDateTime createdAt;
    private int memberCount;
    private Set<Long> memberIds;
    private Set<String> memberNames;
    private int taskCount;

    public static ProjectResponseDTO fromEntity(Project project) {
        if (project == null) return null;

        Set<Long> mIds = project.getUsers() != null
                ? project.getUsers().stream().map(User::getId).collect(Collectors.toSet())
                : Set.of();

        Set<String> mNames = project.getUsers() != null
                ? project.getUsers().stream().map(u -> u.getFirstname() + " " + u.getLastname()).collect(Collectors.toSet())
                : Set.of();

        int tCount = project.getTasks() != null ? project.getTasks().size() : 0;

        return ProjectResponseDTO.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .isArchived(project.getIsArchived() != null ? project.getIsArchived() : false)
                .createdAt(project.getCreatedAt())
                .memberCount(mIds.size())
                .memberIds(mIds)
                .memberNames(mNames)
                .taskCount(tCount)
                .build();
    }
}
