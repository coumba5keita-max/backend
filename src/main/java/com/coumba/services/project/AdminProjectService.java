package com.coumba.services.project;

import com.coumba.dto.project.ProjectRequest;
import com.coumba.dto.project.ProjectResponseDTO;
import com.coumba.dto.user.UserResponseDTO;
import com.coumba.entities.project.Project;
import com.coumba.entities.task.Task;
import com.coumba.entities.user.User;
import com.coumba.exceptions.project.ProjectNotFoundException;
import com.coumba.exceptions.user.UserNotFoundException;
import com.coumba.repositories.notification.NotificationRepository;
import com.coumba.repositories.project.ProjectRepository;
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
public class AdminProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    /**
     * Crée un nouveau projet.
     */
    @Transactional
    public ProjectResponseDTO createProject(ProjectRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] crée le projet [{}]", adminEmail, request.getName());

        String name = request.getName().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("Un projet portant le nom \"" + name + "\" existe déjà.");
        }

        Project project = Project.builder()
                .name(name)
                .description(request.getDescription())
                .isArchived(false)
                .createdAt(LocalDateTime.now())
                .users(new HashSet<>())
                .tasks(new HashSet<>())
                .build();

        Project saved = projectRepository.save(project);
        log.info("AUDIT SUCCÈS : Projet ID [{}] créé par [{}]", saved.getId(), adminEmail);
        return ProjectResponseDTO.fromEntity(saved);
    }

    /**
     * Modifie un projet existant (nom et description).
     */
    @Transactional
    public ProjectResponseDTO updateProject(Long projectId, ProjectRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] modifie le projet ID [{}]", adminEmail, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        String newName = request.getName().trim();
        if (!project.getName().equalsIgnoreCase(newName) && projectRepository.existsByNameIgnoreCase(newName)) {
            throw new IllegalArgumentException("Un autre projet porte déjà le nom \"" + newName + "\".");
        }

        project.setName(newName);
        project.setDescription(request.getDescription());

        Project saved = projectRepository.save(project);
        log.info("AUDIT SUCCÈS : Projet ID [{}] mis à jour par [{}]", saved.getId(), adminEmail);
        return ProjectResponseDTO.fromEntity(saved);
    }

    /**
     * Archive ou désarchive un projet.
     */
    @Transactional
    public ProjectResponseDTO archiveProject(Long projectId, boolean isArchived, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] modifie l'état d'archivage du projet ID [{}] vers [{}]",
                adminEmail, projectId, isArchived);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        project.setIsArchived(isArchived);
        Project saved = projectRepository.save(project);

        String action = isArchived ? "archivé" : "désarchivé";
        log.info("AUDIT SUCCÈS : Projet ID [{}] {} par [{}]", saved.getId(), action, adminEmail);
        return ProjectResponseDTO.fromEntity(saved);
    }

    /**
     * Supprime un projet avec nettoyage préalable des contraintes de clés étrangères.
     */
    @Transactional
    public void deleteProject(Long projectId, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] supprime le projet ID [{}]", adminEmail, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        // 1. Dissocier les notifications de toutes les tâches rattachées au projet
        if (project.getTasks() != null) {
            for (Task task : project.getTasks()) {
                notificationRepository.detachTaskFromNotifications(task.getId());
            }
        }

        // 2. Dissocier les membres du projet
        for (User user : project.getUsers()) {
            user.getProjects().remove(project);
        }
        project.getUsers().clear();

        // 3. Suppression définitive du projet
        projectRepository.delete(project);
        log.info("AUDIT SUCCÈS : Projet ID [{}] supprimé par [{}]", projectId, adminEmail);
    }

    /**
     * Récupère la liste de tous les projets avec filtre d'archivage optionnel.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> getAllProjects(Boolean isArchived) {
        List<Project> projects = isArchived != null
                ? projectRepository.findByIsArchived(isArchived)
                : projectRepository.findAll();

        return projects.stream()
                .map(ProjectResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Récupère les détails d'un projet par ID.
     */
    @Transactional(readOnly = true)
    public ProjectResponseDTO getProjectById(Long projectId) {
        return projectRepository.findById(projectId)
                .map(ProjectResponseDTO::fromEntity)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }

    /**
     * Assigne un utilisateur à un projet spécifique.
     */
    @Transactional
    public void addUserToProject(Long projectId, Long userId, String adminEmail) {
        log.info("AUDIT : Assignation de l'utilisateur ID [{}] au projet ID [{}] par [{}]", userId, projectId, adminEmail);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        project.getUsers().add(user);
        user.getProjects().add(project);

        projectRepository.save(project);
        log.info("AUDIT SUCCÈS : Utilisateur [{}] ajouté au projet [{}]", user.getEmail(), project.getName());
    }

    /**
     * Retire un utilisateur d'un projet.
     */
    @Transactional
    public void removeUserFromProject(Long projectId, Long userId, String adminEmail) {
        log.info("AUDIT : Retrait de l'utilisateur ID [{}] du projet ID [{}] par [{}]", userId, projectId, adminEmail);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        project.getUsers().remove(user);
        user.getProjects().remove(project);

        projectRepository.save(project);
        log.info("AUDIT SUCCÈS : Utilisateur [{}] retiré du projet [{}]", user.getEmail(), project.getName());
    }

    /**
     * Définit en bloc les membres d'un projet.
     */
    @Transactional
    public void setProjectMembers(Long projectId, Set<Long> userIds, String adminEmail) {
        log.info("AUDIT : Mise à jour des membres du projet ID [{}] par [{}]", projectId, adminEmail);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        Set<User> newMembers = new HashSet<>();
        if (userIds != null && !userIds.isEmpty()) {
            for (Long uid : userIds) {
                userRepository.findById(uid).ifPresent(newMembers::add);
            }
        }

        // Dissocier les anciens membres
        for (User currentMember : project.getUsers()) {
            currentMember.getProjects().remove(project);
        }
        project.getUsers().clear();

        // Associer les nouveaux membres
        for (User member : newMembers) {
            project.getUsers().add(member);
            member.getProjects().add(project);
        }

        projectRepository.save(project);
        log.info("AUDIT SUCCÈS : Projet [{}] compte désormais {} membres", project.getName(), newMembers.size());
    }

    /**
     * Récupère la liste des utilisateurs affectés à un projet.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getProjectMembers(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));

        return project.getUsers().stream()
                .map(UserResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
