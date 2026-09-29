package com.coumba.dto.user;

import com.coumba.entities.project.Project;
import com.coumba.entities.team.Team;
import com.coumba.entities.user.User;
import com.coumba.entities.enums.Role;
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
public class UserResponseDTO {

    private Long id;
    private String firstname;
    private String lastname;
    private String email;
    private Role role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Set<Long> teamIds;
    private Set<String> teamNames;
    private Set<Long> projectIds;
    private Set<String> projectNames;

    public static UserResponseDTO fromEntity(User user) {
        if (user == null) return null;

        Set<Long> tIds = user.getTeams() != null
                ? user.getTeams().stream().map(Team::getId).collect(Collectors.toSet())
                : Set.of();
        Set<String> tNames = user.getTeams() != null
                ? user.getTeams().stream().map(Team::getName).collect(Collectors.toSet())
                : Set.of();

        Set<Long> pIds = user.getProjects() != null
                ? user.getProjects().stream().map(Project::getId).collect(Collectors.toSet())
                : Set.of();
        Set<String> pNames = user.getProjects() != null
                ? user.getProjects().stream().map(Project::getName).collect(Collectors.toSet())
                : Set.of();

        return UserResponseDTO.builder()
                .id(user.getId())
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .teamIds(tIds)
                .teamNames(tNames)
                .projectIds(pIds)
                .projectNames(pNames)
                .build();
    }
}
