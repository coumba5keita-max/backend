package com.coumba.services.project;

import com.coumba.dto.user.UserResponseDTO;
import com.coumba.entities.project.Project;
import com.coumba.entities.user.User;
import com.coumba.repositories.project.ProjectRepository;
import com.coumba.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Assigne un utilisateur à un projet spécifique.
     */
    @Transactional
    public void addUserToProject(Long projectId, Long userId, String adminEmail) {
        log.info("AUDIT : Assignation de l'utilisateur ID [{}] au projet ID [{}] par [{}]", userId, projectId, adminEmail);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable avec l'ID : " + projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));

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
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable avec l'ID : " + projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable avec l'ID : " + userId));

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
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable avec l'ID : " + projectId));

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
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable avec l'ID : " + projectId));

        return project.getUsers().stream()
                .map(UserResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
