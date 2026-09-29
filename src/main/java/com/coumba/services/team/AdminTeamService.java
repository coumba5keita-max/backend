package com.coumba.services.team;

import com.coumba.dto.team.TeamRequest;
import com.coumba.dto.team.TeamResponseDTO;
import com.coumba.dto.user.UserResponseDTO;
import com.coumba.entities.team.Team;
import com.coumba.entities.user.User;
import com.coumba.repositories.team.TeamRepository;
import com.coumba.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    /**
     * Crée une nouvelle équipe.
     */
    @Transactional
    public TeamResponseDTO createTeam(TeamRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] crée l'équipe [{}]", adminEmail, request.getName());

        if (teamRepository.existsByName(request.getName().trim())) {
            throw new IllegalArgumentException("Une équipe avec le nom \"" + request.getName() + "\" existe déjà.");
        }

        Team team = Team.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .users(new HashSet<>())
                .build();

        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            for (Long userId : request.getUserIds()) {
                userRepository.findById(userId).ifPresent(user -> {
                    team.getUsers().add(user);
                    user.getTeams().add(team);
                });
            }
        }

        Team savedTeam = teamRepository.save(team);
        log.info("AUDIT SUCCÈS : Équipe ID [{}] créée par [{}]", savedTeam.getId(), adminEmail);
        return TeamResponseDTO.fromEntity(savedTeam);
    }

    /**
     * Modifie le nom et la description d'une équipe.
     */
    @Transactional
    public TeamResponseDTO updateTeam(Long teamId, TeamRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] modifie l'équipe ID [{}]", adminEmail, teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable avec l'ID : " + teamId));

        String newName = request.getName().trim();
        if (!team.getName().equalsIgnoreCase(newName) && teamRepository.existsByName(newName)) {
            throw new IllegalArgumentException("Une autre équipe porte déjà le nom \"" + newName + "\".");
        }

        team.setName(newName);
        team.setDescription(request.getDescription());

        Team updatedTeam = teamRepository.save(team);
        log.info("AUDIT SUCCÈS : Équipe ID [{}] modifiée par [{}]", updatedTeam.getId(), adminEmail);
        return TeamResponseDTO.fromEntity(updatedTeam);
    }

    /**
     * Supprime une équipe.
     */
    @Transactional
    public void deleteTeam(Long teamId, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] supprime l'équipe ID [{}]", adminEmail, teamId);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable avec l'ID : " + teamId));

        // Dissocier les utilisateurs pour éviter les contraintes de clés étrangères
        for (User user : team.getUsers()) {
            user.getTeams().remove(team);
        }
        team.getUsers().clear();

        teamRepository.delete(team);
        log.info("AUDIT SUCCÈS : Équipe ID [{}] supprimée par [{}]", teamId, adminEmail);
    }

    /**
     * Assigne un utilisateur à une équipe spécifique.
     */
    @Transactional
    public TeamResponseDTO addUserToTeam(Long teamId, Long userId, String adminEmail) {
        log.info("AUDIT : Assignation de l'utilisateur ID [{}] à l'équipe ID [{}] par [{}]", userId, teamId, adminEmail);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable avec l'ID : " + teamId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));

        team.getUsers().add(user);
        user.getTeams().add(team);

        Team savedTeam = teamRepository.save(team);
        log.info("AUDIT SUCCÈS : Utilisateur [{}] ajouté à l'équipe [{}]", user.getEmail(), team.getName());
        return TeamResponseDTO.fromEntity(savedTeam);
    }

    /**
     * Retire un utilisateur d'une équipe.
     */
    @Transactional
    public TeamResponseDTO removeUserFromTeam(Long teamId, Long userId, String adminEmail) {
        log.info("AUDIT : Retrait de l'utilisateur ID [{}] de l'équipe ID [{}] par [{}]", userId, teamId, adminEmail);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable avec l'ID : " + teamId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));

        team.getUsers().remove(user);
        user.getTeams().remove(team);

        Team savedTeam = teamRepository.save(team);
        log.info("AUDIT SUCCÈS : Utilisateur [{}] retiré de l'équipe [{}]", user.getEmail(), team.getName());
        return TeamResponseDTO.fromEntity(savedTeam);
    }

    /**
     * Définit en bloc les membres d'une équipe.
     */
    @Transactional
    public TeamResponseDTO setTeamMembers(Long teamId, Set<Long> userIds, String adminEmail) {
        log.info("AUDIT : Mise à jour en bloc des membres de l'équipe ID [{}] par [{}]", teamId, adminEmail);

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable avec l'ID : " + teamId));

        Set<User> newMembers = new HashSet<>();
        if (userIds != null && !userIds.isEmpty()) {
            for (Long uid : userIds) {
                userRepository.findById(uid).ifPresent(newMembers::add);
            }
        }

        // Nettoyer les anciennes associations
        for (User currentMember : team.getUsers()) {
            currentMember.getTeams().remove(team);
        }
        team.getUsers().clear();

        // Appliquer les nouveaux membres
        for (User member : newMembers) {
            team.getUsers().add(member);
            member.getTeams().add(team);
        }

        Team savedTeam = teamRepository.save(team);
        log.info("AUDIT SUCCÈS : Équipe [{}] compte désormais {} membres", team.getName(), savedTeam.getUsers().size());
        return TeamResponseDTO.fromEntity(savedTeam);
    }

    /**
     * Récupère la liste des membres d'une équipe.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable avec l'ID : " + teamId));

        return team.getUsers().stream()
                .map(UserResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère toutes les équipes.
     */
    @Transactional(readOnly = true)
    public List<TeamResponseDTO> getAllTeams() {
        return teamRepository.findAll()
                .stream()
                .map(TeamResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère une équipe par ID.
     */
    @Transactional(readOnly = true)
    public TeamResponseDTO getTeamById(Long teamId) {
        return teamRepository.findById(teamId)
                .map(TeamResponseDTO::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable avec l'ID : " + teamId));
    }
}
