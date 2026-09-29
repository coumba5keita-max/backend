package com.coumba.services;

import com.coumba.dto.InviteUserRequest;
import com.coumba.dto.UpdateUserRequest;
import com.coumba.dto.UserResponseDTO;
import com.coumba.entities.Project;
import com.coumba.entities.Team;
import com.coumba.entities.User;
import com.coumba.entities.enums.Role;
import com.coumba.repositories.ProjectRepository;
import com.coumba.repositories.TeamRepository;
import com.coumba.repositories.UserRepository;
import com.coumba.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class AdminUserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Invite un nouvel utilisateur dans le système :
     * - Génération cryptographique d'un mot de passe temporaire fort.
     * - Hachage BCrypt.
     * - Association immédiate aux équipes et projets spécifiés.
     * - Envoi d'un email d'invitation avec les consignes de sécurité.
     */
    @Transactional
    public UserResponseDTO inviteUser(InviteUserRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] initie l'invitation de l'utilisateur [{}]", adminEmail, request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un compte avec l'adresse email " + request.getEmail() + " existe déjà.");
        }

        // 1. Génération cryptographique d'un mot de passe temporaire
        String temporaryPassword = SecurityUtils.generateSecureTemporaryPassword();

        // 2. Création de l'entité User
        User user = User.builder()
                .firstname(request.getFirstname().trim())
                .lastname(request.getLastname().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(temporaryPassword))
                .role(request.getRole() != null ? request.getRole() : Role.ROLE_USER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .teams(new HashSet<>())
                .projects(new HashSet<>())
                .build();

        // 3. Association aux équipes si spécifiées
        if (request.getTeamIds() != null && !request.getTeamIds().isEmpty()) {
            for (Long teamId : request.getTeamIds()) {
                teamRepository.findById(teamId).ifPresent(team -> {
                    team.getUsers().add(user);
                    user.getTeams().add(team);
                });
            }
        }

        // 4. Association aux projets si spécifiés
        if (request.getProjectIds() != null && !request.getProjectIds().isEmpty()) {
            for (Long projectId : request.getProjectIds()) {
                projectRepository.findById(projectId).ifPresent(project -> {
                    project.getUsers().add(user);
                    user.getProjects().add(project);
                });
            }
        }

        User savedUser = userRepository.save(user);

        // 5. Envoi de l'email d'invitation
        emailService.sendUserInvitationEmail(savedUser.getEmail(), temporaryPassword, adminEmail);

        log.info("AUDIT SUCCÈS : Utilisateur [{}] (ID: {}) créé et invité par [{}] avec le rôle [{}]",
                savedUser.getEmail(), savedUser.getId(), adminEmail, savedUser.getRole());

        return UserResponseDTO.fromEntity(savedUser);
    }

    /**
     * Modifie les informations d'un utilisateur :
     * - Prénom, nom, email, rôle.
     * - Contrôle de sécurité pour empêcher la rétrogradation du dernier administrateur.
     */
    @Transactional
    public UserResponseDTO updateUser(Long userId, UpdateUserRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] modifie l'utilisateur ID [{}]", adminEmail, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));

        // Vérification d'unicité de l'email si modifié
        String newEmail = request.getEmail().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("L'adresse email " + newEmail + " est déjà utilisée par un autre compte.");
        }

        // Contrôle de sécurité : interdiction de rétrograder le dernier administrateur actif
        if (user.getRole() == Role.ROLE_ADMIN && request.getRole() != Role.ROLE_ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndIsActiveTrue(Role.ROLE_ADMIN);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Opération interdite : impossible de retirer le rôle ADMIN au dernier administrateur actif du système.");
            }
        }

        user.setFirstname(request.getFirstname().trim());
        user.setLastname(request.getLastname().trim());
        user.setEmail(newEmail);
        user.setRole(request.getRole());

        User updatedUser = userRepository.save(user);

        log.info("AUDIT SUCCÈS : Utilisateur ID [{}] mis à jour par [{}] : email=[{}], role=[{}]",
                updatedUser.getId(), adminEmail, updatedUser.getEmail(), updatedUser.getRole());

        return UserResponseDTO.fromEntity(updatedUser);
    }

    /**
     * Active ou désactive un compte utilisateur :
     * - Empêche un administrateur de désactiver son propre compte (Self-lockout prevention).
     * - Empêche la désactivation du dernier administrateur actif.
     */
    @Transactional
    public UserResponseDTO toggleUserStatus(Long userId, boolean active, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] modifie le statut d'activation de l'utilisateur ID [{}] vers [{}]",
                adminEmail, userId, active);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));

        // Sécurité : Un administrateur ne peut pas désactiver son propre compte
        if (user.getEmail().equalsIgnoreCase(adminEmail) && !active) {
            throw new AccessDeniedException("Opération refusée : vous ne pouvez pas désactiver votre propre compte administrateur.");
        }

        // Sécurité : Ne pas désactiver le dernier administrateur actif
        if (user.getRole() == Role.ROLE_ADMIN && !active) {
            long activeAdminCount = userRepository.countByRoleAndIsActiveTrue(Role.ROLE_ADMIN);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Opération interdite : impossible de désactiver le dernier administrateur actif du système.");
            }
        }

        user.setIsActive(active);
        User savedUser = userRepository.save(user);

        // Notification par email de la modification de statut
        emailService.sendAccountStatusChangeEmail(savedUser.getEmail(), active, adminEmail);

        log.info("AUDIT SUCCÈS : Statut du compte [{}] modifié à [Actif={}] par [{}]",
                savedUser.getEmail(), active, adminEmail);

        return UserResponseDTO.fromEntity(savedUser);
    }

    /**
     * Récupère la liste de tous les utilisateurs.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les détails d'un utilisateur par son ID.
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponseDTO::fromEntity)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));
    }

    /**
     * Assigne un utilisateur à une équipe.
     */
    @Transactional
    public UserResponseDTO assignUserToTeam(Long userId, Long teamId, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] assigne l'utilisateur ID [{}] à l'équipe ID [{}]", adminEmail, userId, teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable avec l'ID : " + teamId));

        team.getUsers().add(user);
        user.getTeams().add(team);
        teamRepository.save(team);
        User savedUser = userRepository.save(user);

        log.info("AUDIT SUCCÈS : Utilisateur [{}] ajouté à l'équipe [{}]", user.getEmail(), team.getName());
        return UserResponseDTO.fromEntity(savedUser);
    }

    /**
     * Retire un utilisateur d'une équipe.
     */
    @Transactional
    public UserResponseDTO removeUserFromTeam(Long userId, Long teamId, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] retire l'utilisateur ID [{}] de l'équipe ID [{}]", adminEmail, userId, teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Équipe introuvable avec l'ID : " + teamId));

        team.getUsers().remove(user);
        user.getTeams().remove(team);
        teamRepository.save(team);
        User savedUser = userRepository.save(user);

        log.info("AUDIT SUCCÈS : Utilisateur [{}] retiré de l'équipe [{}]", user.getEmail(), team.getName());
        return UserResponseDTO.fromEntity(savedUser);
    }

    /**
     * Assigne un utilisateur à un projet.
     */
    @Transactional
    public UserResponseDTO assignUserToProject(Long userId, Long projectId, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] assigne l'utilisateur ID [{}] au projet ID [{}]", adminEmail, userId, projectId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable avec l'ID : " + projectId));

        project.getUsers().add(user);
        user.getProjects().add(project);
        projectRepository.save(project);
        User savedUser = userRepository.save(user);

        log.info("AUDIT SUCCÈS : Utilisateur [{}] ajouté au projet [{}]", user.getEmail(), project.getName());
        return UserResponseDTO.fromEntity(savedUser);
    }

    /**
     * Retire un utilisateur d'un projet.
     */
    @Transactional
    public UserResponseDTO removeUserFromProject(Long userId, Long projectId, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] retire l'utilisateur ID [{}] du projet ID [{}]", adminEmail, userId, projectId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable avec l'ID : " + projectId));

        project.getUsers().remove(user);
        user.getProjects().remove(project);
        projectRepository.save(project);
        User savedUser = userRepository.save(user);

        log.info("AUDIT SUCCÈS : Utilisateur [{}] retiré du projet [{}]", user.getEmail(), project.getName());
        return UserResponseDTO.fromEntity(savedUser);
    }
}
