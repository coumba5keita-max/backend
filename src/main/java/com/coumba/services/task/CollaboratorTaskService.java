package com.coumba.services.task;

import com.coumba.dto.task.LogTaskTimeRequest;
import com.coumba.dto.task.TaskResponseDTO;
import com.coumba.entities.enums.Role;
import com.coumba.entities.enums.TaskPriority;
import com.coumba.entities.enums.TaskStatus;
import com.coumba.entities.project.Project;
import com.coumba.entities.task.Task;
import com.coumba.entities.user.User;
import com.coumba.exceptions.task.TaskNotFoundException;
import com.coumba.exceptions.user.UserNotFoundException;
import com.coumba.repositories.project.ProjectRepository;
import com.coumba.repositories.task.TaskRepository;
import com.coumba.repositories.user.UserRepository;
import com.coumba.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollaboratorTaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final NotificationService notificationService;

    /**
     * Recherche et filtrage des tâches attribuées au collaborateur ou rattachées à ses projets assignés.
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getMyTasks(
            String userEmail,
            String search,
            TaskStatus status,
            TaskPriority priority,
            Long projectId,
            Long categoryId,
            Boolean overdue
    ) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userEmail));

        Long userId = user.getId();
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        // Récupérer les IDs des projets assignés à l'utilisateur
        Set<Long> userProjectIds = projectRepository.findByUserId(userId).stream()
                .filter(p -> p.getIsArchived() == null || !p.getIsArchived())
                .map(Project::getId)
                .collect(Collectors.toSet());
        if (user.getProjects() != null) {
            user.getProjects().stream()
                    .filter(p -> p.getIsArchived() == null || !p.getIsArchived())
                    .forEach(p -> userProjectIds.add(p.getId()));
        }

        LocalDate today = LocalDate.now();
        String searchLower = search != null ? search.trim().toLowerCase() : null;

        return taskRepository.findAll().stream()
                // Doit être assigné directement, créé par lui, appartenir à un de ses projets assignés, ou l'utilisateur est admin
                .filter(t -> {
                    if (isAdmin) return true;
                    boolean isAssignee = t.getAssignees() != null && t.getAssignees().stream().anyMatch(a -> a.getId().equals(userId));
                    boolean isCreator = t.getCreator() != null && t.getCreator().getId().equals(userId);
                    boolean isProjectMember = t.getProject() != null && userProjectIds.contains(t.getProject().getId());
                    return isAssignee || isCreator || isProjectMember;
                })
                // Recherche par mot-clé (titre ou description)
                .filter(t -> searchLower == null || searchLower.isEmpty() ||
                        (t.getTitle() != null && t.getTitle().toLowerCase().contains(searchLower)) ||
                        (t.getDescription() != null && t.getDescription().toLowerCase().contains(searchLower)))
                // Filtre par statut
                .filter(t -> status == null || t.getStatus() == status)
                // Filtre par priorité
                .filter(t -> priority == null || t.getPriority() == priority)
                // Filtre par projet
                .filter(t -> projectId == null || (t.getProject() != null && t.getProject().getId().equals(projectId)))
                // Filtre par catégorie
                .filter(t -> categoryId == null || (t.getCategory() != null && t.getCategory().getId().equals(categoryId)))
                // Filtre par retard
                .filter(t -> {
                    if (overdue == null) return true;
                    boolean isTaskOverdue = t.getDueDate() != null
                            && t.getDueDate().isBefore(today)
                            && t.getStatus() != TaskStatus.TERMINE
                            && t.getStatus() != TaskStatus.CLOTURE;
                    return overdue.equals(isTaskOverdue);
                })
                .map(TaskResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Mise à jour de l'état d'avancement d'une tâche assignée (À faire → En cours → Terminé).
     */
    @Transactional
    public TaskResponseDTO updateMyTaskStatus(Long taskId, TaskStatus newStatus, String userEmail) {
        log.info("L'utilisateur [{}] met à jour le statut de la tâche ID [{}] vers [{}]", userEmail, taskId, newStatus);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userEmail));

        boolean isAssignee = task.getAssignees() != null && task.getAssignees().stream().anyMatch(a -> a.getId().equals(user.getId()));
        boolean isCreator = task.getCreator() != null && task.getCreator().getId().equals(user.getId());
        boolean isProjectMember = task.getProject() != null && (
                projectRepository.findByUserId(user.getId()).stream().anyMatch(p -> p.getId().equals(task.getProject().getId())) ||
                (user.getProjects() != null && user.getProjects().stream().anyMatch(p -> p.getId().equals(task.getProject().getId())))
        );
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        // Contrôle de sécurité : l'utilisateur doit être assigné ou créateur ou membre du projet ou admin
        if (!isAssignee && !isCreator && !isProjectMember && !isAdmin) {
            throw new AccessDeniedException("Vous n'êtes pas assigné à cette tâche ni membre de son projet.");
        }

        // Seul un admin peut clôturer définitivement un ticket (CLOTURE)
        if (newStatus == TaskStatus.CLOTURE && !isAdmin) {
            throw new AccessDeniedException("La clôture définitive des tickets est réservée aux responsables/administrateurs.");
        }

        TaskStatus previousStatus = task.getStatus();
        task.setStatus(newStatus);

        if (newStatus == TaskStatus.TERMINE || newStatus == TaskStatus.CLOTURE) {
            task.setClosedAt(LocalDateTime.now());
        }

        Task saved = taskRepository.save(task);

        // Avertir le créateur de la tâche si le statut a changé
        if (previousStatus != newStatus && task.getCreator() != null && !task.getCreator().getId().equals(user.getId())) {
            String updaterFullName = user.getFirstname() + " " + user.getLastname();
            notificationService.notifyTaskStatusChange(task.getCreator(), saved, newStatus, updaterFullName);
        }

        log.info("Statut de la tâche ID [{}] mis à jour avec succès vers [{}]", taskId, newStatus);
        return TaskResponseDTO.fromEntity(saved);
    }

    /**
     * Estimation ou saisie du temps passé sur un ticket.
     */
    @Transactional
    public TaskResponseDTO logTaskTime(Long taskId, LogTaskTimeRequest request, String userEmail) {
        log.info("Saisie de temps pour la tâche ID [{}] par [{}]", taskId, userEmail);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userEmail));

        boolean isAssignee = task.getAssignees() != null && task.getAssignees().stream().anyMatch(a -> a.getId().equals(user.getId()));
        boolean isCreator = task.getCreator() != null && task.getCreator().getId().equals(user.getId());
        boolean isProjectMember = task.getProject() != null && (
                projectRepository.findByUserId(user.getId()).stream().anyMatch(p -> p.getId().equals(task.getProject().getId())) ||
                (user.getProjects() != null && user.getProjects().stream().anyMatch(p -> p.getId().equals(task.getProject().getId())))
        );
        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;

        if (!isAssignee && !isCreator && !isProjectMember && !isAdmin) {
            throw new AccessDeniedException("Vous n'êtes pas assigné à cette tâche et ne pouvez pas enregistrer de temps de travail.");
        }

        // Mise à jour du temps estimé
        if (request.getEstimatedTime() != null) {
            if (request.getEstimatedTime() < 0) {
                throw new IllegalArgumentException("Le temps estimé ne peut pas être négatif.");
            }
            task.setEstimatedTime(request.getEstimatedTime());
        }

        // Mise à jour du temps passé
        if (request.getSpentTime() != null) {
            if (request.getSpentTime() < 0) {
                throw new IllegalArgumentException("Le temps passé ne peut pas être négatif.");
            }
            task.setSpentTime(request.getSpentTime());
        } else if (request.getAdditionalSpentTime() != null) {
            if (request.getAdditionalSpentTime() < 0) {
                throw new IllegalArgumentException("Le temps additionnel ne peut pas être négatif.");
            }
            double currentSpent = task.getSpentTime() != null ? task.getSpentTime() : 0.0;
            task.setSpentTime(currentSpent + request.getAdditionalSpentTime());
        }

        Task saved = taskRepository.save(task);
        log.info("Temps mis à jour pour la tâche ID [{}] : estimé={}h, passé={}h",
                taskId, saved.getEstimatedTime(), saved.getSpentTime());

        return TaskResponseDTO.fromEntity(saved);
    }
}
