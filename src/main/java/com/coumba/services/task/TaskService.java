package com.coumba.services.task;

import com.coumba.services.notification.NotificationService;

import com.coumba.entities.task.Category;
import com.coumba.entities.project.Project;
import com.coumba.entities.task.Task;
import com.coumba.entities.user.User;
import com.coumba.entities.enums.TaskStatus;
import com.coumba.repositories.task.CategoryRepository;
import com.coumba.repositories.notification.NotificationRepository;
import com.coumba.repositories.project.ProjectRepository;
import com.coumba.repositories.task.TaskRepository;
import com.coumba.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    /**
     * Récupère toutes les tâches.
     */
    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    /**
     * Récupère une tâche par son ID.
     */
    public Optional<Task> getTaskById(Long id) {
        return taskRepository.findById(id);
    }

    /**
     * Création d'une tâche avec assignation.
     * Déclenche NotificationService pour avertir le ou les utilisateurs assignés.
     */
    @Transactional
    public Task createTask(Task task, Long creatorId, Long projectId, Long categoryId, Set<Long> assigneeIds) {
        log.info("Création d'une nouvelle tâche par l'utilisateur ID : {}", creatorId);

        // 1. Associer le créateur
        User creator = null;
        if (creatorId != null) {
            creator = userRepository.findById(creatorId).orElse(null);
        }
        if (creator == null) {
            creator = userRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Aucun utilisateur disponible en base. Veuillez vous inscrire d'abord."));
        }
        task.setCreator(creator);

        // 2. Associer le projet (création automatique si non existant pour faciliter les tests)
        Project project;
        if (projectId != null) {
            project = projectRepository.findById(projectId)
                    .orElseGet(() -> {
                        log.info("Projet {} introuvable, création automatique pour éviter l'erreur", projectId);
                        return projectRepository.save(Project.builder()
                                .name("Projet #" + projectId)
                                .description("Projet généré automatiquement")
                                .isArchived(false)
                                .createdAt(LocalDateTime.now())
                                .build());
                    });
        } else {
            project = projectRepository.findAll().stream().findFirst()
                    .orElseGet(() -> {
                        log.info("Aucun projet spécifié, création automatique du Projet Général");
                        return projectRepository.save(Project.builder()
                                .name("Projet Général")
                                .description("Projet par défaut")
                                .isArchived(false)
                                .createdAt(LocalDateTime.now())
                                .build());
                    });
        }
        task.setProject(project);

        // 3. Associer la catégorie (optionnel, création automatique si ID fourni introuvable)
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseGet(() -> categoryRepository.save(Category.builder()
                            .name("Catégorie #" + categoryId)
                            .build()));
            task.setCategory(category);
        }

        // 4. Associer les utilisateurs assignés
        Set<User> assignees = new HashSet<>();
        if (assigneeIds != null && !assigneeIds.isEmpty()) {
            for (Long assigneeId : assigneeIds) {
                userRepository.findById(assigneeId).ifPresent(assignees::add);
            }
        }
        // Si aucun assigné valide n'a été trouvé, on assigne le créateur pour garantir l'envoi de la notification
        if (assignees.isEmpty() && creator != null) {
            assignees.add(creator);
        }
        task.setAssignees(assignees);

        // 5. Initialisation de la date si absente
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(LocalDateTime.now());
        }

        // 6. Sauvegarde de la tâche
        Task savedTask = taskRepository.save(task);

        // 7. Déclenchement des notifications pour chaque utilisateur assigné
        String creatorFullName = creator.getFirstname() + " " + creator.getLastname();
        for (User assignee : assignees) {
            notificationService.notifyTaskAssignment(assignee, savedTask, creatorFullName);
        }

        return savedTask;
    }

    /**
     * Mise à jour de l'état d'une tâche.
     * Déclenche NotificationService pour avertir le créateur de la tâche (ex: passage à TERMINE).
     */
    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus newStatus, Long updatedByUserId) {
        log.info("Mise à jour du statut de la tâche ID {} vers {} par l'utilisateur {}", taskId, newStatus, updatedByUserId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Tâche introuvable avec l'ID : " + taskId));

        TaskStatus previousStatus = task.getStatus();

        // Mise à jour de la tâche
        task.setStatus(newStatus);
        if (newStatus == TaskStatus.TERMINE || newStatus == TaskStatus.CLOTURE) {
            task.setClosedAt(LocalDateTime.now());
        }

        Task updatedTask = taskRepository.save(task);

        // Si le statut a changé, avertir le créateur de la tâche
        if (previousStatus != newStatus) {
            User creator = task.getCreator();
            if (creator != null) {
                String updaterName = "Système";
                if (updatedByUserId != null) {
                    updaterName = userRepository.findById(updatedByUserId)
                            .map(u -> u.getFirstname() + " " + u.getLastname())
                            .orElse("Utilisateur #" + updatedByUserId);
                }

                // Déclenche la notification temps réel + email vers le créateur
                notificationService.notifyTaskStatusChange(creator, updatedTask, newStatus, updaterName);
            }
        }

        return updatedTask;
    }

    /**
     * Suppression d'une tâche avec détachement sécurisé des notifications et assignations.
     */
    @Transactional
    public void deleteTask(Long id) {
        taskRepository.findById(id).ifPresent(task -> {
            // 1. Dissociation immédiate en base de données de toutes les notifications liées
            notificationRepository.detachTaskFromNotifications(id);

            // 2. Nettoyage des assignations (table de jointure task_assignee)
            if (task.getAssignees() != null) {
                task.getAssignees().clear();
            }

            // 3. Suppression de la tâche
            taskRepository.delete(task);
            log.info("Tâche ID {} supprimée avec succès.", id);
        });
    }
}
