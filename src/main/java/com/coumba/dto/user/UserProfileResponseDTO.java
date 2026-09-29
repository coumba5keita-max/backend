package com.coumba.dto.user;

import com.coumba.entities.enums.Role;
import com.coumba.entities.project.Project;
import com.coumba.entities.team.Team;
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
public class UserProfileResponseDTO {

    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private Role role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private int assignedTasksCount;
    private Set<String> projectNames;
    private Set<String> teamNames;

    public static UserProfileResponseDTO fromEntity(User user) {
        if (user == null) return null;

        Set<String> projects = user.getProjects() != null
                ? user.getProjects().stream().map(Project::getName).collect(Collectors.toSet())
                : Set.of();

        Set<String> teams = user.getTeams() != null
                ? user.getTeams().stream().map(Team::getName).collect(Collectors.toSet())
                : Set.of();

        int tasksCount = user.getAssignedTasks() != null ? user.getAssignedTasks().size() : 0;

        return UserProfileResponseDTO.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .assignedTasksCount(tasksCount)
                .projectNames(projects)
                .teamNames(teams)
                .build();
    }
}
