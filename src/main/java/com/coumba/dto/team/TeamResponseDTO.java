package com.coumba.dto.team;

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
public class TeamResponseDTO {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private int memberCount;
    private Set<Long> memberIds;
    private Set<String> memberNames;

    public static TeamResponseDTO fromEntity(Team team) {
        if (team == null) return null;

        Set<Long> mIds = team.getUsers() != null
                ? team.getUsers().stream().map(User::getId).collect(Collectors.toSet())
                : Set.of();
        Set<String> mNames = team.getUsers() != null
                ? team.getUsers().stream().map(u -> u.getFirstname() + " " + u.getLastname()).collect(Collectors.toSet())
                : Set.of();

        return TeamResponseDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .createdAt(team.getCreatedAt())
                .memberCount(mIds.size())
                .memberIds(mIds)
                .memberNames(mNames)
                .build();
    }
}
