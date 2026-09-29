package com.coumba.services.task;

import com.coumba.dto.task.CreateTaskRequest;
import com.coumba.dto.task.ReassignTaskRequest;
import com.coumba.dto.task.TaskResponseDTO;
import com.coumba.dto.task.UpdateDueDateRequest;
import com.coumba.dto.task.CloseTicketRequest;
import com.coumba.entities.enums.TaskPriority;
import com.coumba.entities.enums.TaskStatus;
import com.coumba.entities.project.Project;
import com.coumba.entities.task.Category;
import com.coumba.entities.task.Task;
import com.coumba.entities.user.User;
import com.coumba.exceptions.project.ProjectNotFoundException;
import com.coumba.exceptions.task.TaskNotFoundException;
import com.coumba.repositories.project.ProjectRepository;
import com.coumba.repositories.task.CategoryRepository;
import com.coumba.repositories.task.TaskRepository;
import com.coumba.repositories.user.UserRepository;
import com.coumba.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class AdminTaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    /**
     * Création d'une tâche par l'administrateur avec attribution à un ou plusieurs collaborateurs
     * (y compris lui-même ou d'autres membres).
     */
    @Transactional
    public TaskResponseDTO createTask(CreateTaskRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] crée la tâche/ticket [{}]", adminEmail, request.getTitle());

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("Administrateur introuvable avec l'email : " + adminEmail));

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new ProjectNotFoundException(request.getProjectId()));

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId()).orElse(null);
        }

        Set<User> assignees = new HashSet<>();
        if (request.getAssigneeIds() != null && !request.getAssigneeIds().isEmpty()) {
            for (Long uid : request.getAssigneeIds()) {
                userRepository.findById(uid).ifPresent(assignees::add);
            }
        }
        // Si aucun assigné spécifié, on assigne l'administrateur créateur
        if (assignees.isEmpty()) {
            assignees.add(admin);
        }

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : TaskStatus.A_FAIRE)
                .priority(request.getPriority() != null ? request.getPriority() : TaskPriority.MOYENNE)
                .dueDate(request.getDueDate())
                .estimatedTime(request.getEstimatedTime())
                .spentTime(request.getSpentTime() != null ? request.getSpentTime() : 0.0)
                .createdAt(LocalDateTime.now())
                .project(project)
                .category(category)
                .creator(admin)
                .assignees(assignees)
                .build();

        Task saved = taskRepository.save(task);

        // Notifier tous les collaborateurs assignés
        String adminFullName = admin.getFirstname() + " " + admin.getLastname();
        for (User assignee : assignees) {
            notificationService.notifyTaskAssignment(assignee, saved, adminFullName);
        }

        log.info("AUDIT SUCCÈS : Tâche ID [{}] créée avec succès avec {} assigné(s)", saved.getId(), assignees.size());
        return TaskResponseDTO.fromEntity(saved);
    }

    /**
     * Réaffectation d'une tâche d'un membre à un ou plusieurs autres collaborateurs.
     */
    @Transactional
    public TaskResponseDTO reassignTask(Long taskId, ReassignTaskRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] réaffecte la tâche ID [{}]", adminEmail, taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        User admin = userRepository.findByEmail(adminEmail)
                .orElse(null);
        String adminFullName = admin != null ? admin.getFirstname() + " " + admin.getLastname() : "L'administrateur";

        Set<Long> previousAssigneeIds = task.getAssignees().stream().map(User::getId).collect(Collectors.toSet());
        Set<User> newAssignees = new HashSet<>();

        for (Long uid : request.getAssigneeIds()) {
            userRepository.findById(uid).ifPresent(newAssignees::add);
        }

        if (newAssignees.isEmpty()) {
            throw new IllegalArgumentException("Aucun collaborateur valide trouvé dans la liste des identifiants fournis.");
        }

        task.getAssignees().clear();
        task.getAssignees().addAll(newAssignees);

        Task saved = taskRepository.save(task);

        // Notifier les nouveaux collaborateurs affectés
        for (User newAssignee : newAssignees) {
            if (!previousAssigneeIds.contains(newAssignee.getId())) {
                notificationService.notifyTaskAssignment(newAssignee, saved, adminFullName + " (Réaffectation)");
            }
        }

        log.info("AUDIT SUCCÈS : Tâche ID [{}] réaffectée à {} collaborateur(s)", taskId, newAssignees.size());
        return TaskResponseDTO.fromEntity(saved);
    }

    /**
     * Modification de la date d'échéance (Due date) d'une tâche/ticket.
     */
    @Transactional
    public TaskResponseDTO updateDueDate(Long taskId, UpdateDueDateRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] modifie l'échéance de la tâche ID [{}] vers [{}]",
                adminEmail, taskId, request.getDueDate());

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        String adminFullName = admin != null ? admin.getFirstname() + " " + admin.getLastname() : "L'administrateur";

        task.setDueDate(request.getDueDate());
        Task saved = taskRepository.save(task);

        // Notifier tous les collaborateurs assignés de la nouvelle échéance
        if (task.getAssignees() != null) {
            for (User assignee : task.getAssignees()) {
                notificationService.notifyTaskDueDateChange(assignee, saved, request.getDueDate(), adminFullName);
            }
        }

        log.info("AUDIT SUCCÈS : Date d'échéance de la tâche ID [{}] mise à jour au [{}]", taskId, request.getDueDate());
        return TaskResponseDTO.fromEntity(saved);
    }

    /**
     * Clôture définitive d'un ticket.
     */
    @Transactional
    public TaskResponseDTO closeTicket(Long taskId, CloseTicketRequest request, String adminEmail) {
        log.info("AUDIT : L'administrateur [{}] clôture définitivement le ticket ID [{}]", adminEmail, taskId);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        String adminFullName = admin != null ? admin.getFirstname() + " " + admin.getLastname() : "L'administrateur";

        task.setStatus(TaskStatus.CLOTURE);
        task.setClosedAt(LocalDateTime.now());

        if (request != null && request.getResolutionNote() != null && !request.getResolutionNote().isBlank()) {
            String noteSuffix = "\n\n[CLÔTURE PAR " + adminFullName + " le " + LocalDateTime.now() + "] : " + request.getResolutionNote();
            task.setDescription((task.getDescription() != null ? task.getDescription() : "") + noteSuffix);
        }

        Task saved = taskRepository.save(task);

        // Notifier tous les collaborateurs assignés et le créateur de la clôture
        Set<User> recipients = new HashSet<>();
        if (task.getAssignees() != null) recipients.addAll(task.getAssignees());
        if (task.getCreator() != null) recipients.add(task.getCreator());

        String note = request != null ? request.getResolutionNote() : null;
        for (User recipient : recipients) {
            notificationService.notifyTicketClosed(recipient, saved, adminFullName, note);
        }

        log.info("AUDIT SUCCÈS : Ticket ID [{}] définitivement clôturé par [{}]", taskId, adminEmail);
        return TaskResponseDTO.fromEntity(saved);
    }

    /**
     * Récupère la liste des tâches avec filtres d'administration.
     */
    @Transactional(readOnly = true)
    public List<TaskResponseDTO> getAllAdminTasks(
            TaskStatus status,
            TaskPriority priority,
            Long projectId,
            Long assigneeId,
            Boolean overdue
    ) {
        LocalDate today = LocalDate.now();

        return taskRepository.findAll().stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> projectId == null || (t.getProject() != null && t.getProject().getId().equals(projectId)))
                .filter(t -> assigneeId == null || (t.getAssignees() != null && t.getAssignees().stream().anyMatch(a -> a.getId().equals(assigneeId))))
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
}
