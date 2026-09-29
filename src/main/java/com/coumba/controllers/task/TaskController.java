package com.coumba.controllers.task;

import com.coumba.dto.task.CreateTaskRequest;
import com.coumba.dto.task.TaskResponseDTO;
import com.coumba.dto.task.UpdateTaskStatusRequest;
import com.coumba.entities.task.Task;
import com.coumba.entities.user.User;
import com.coumba.repositories.user.UserRepository;
import com.coumba.security.CustomUserDetails;
import com.coumba.services.task.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    /**
     * GET /api/tasks : Récupérer toutes les tâches.
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getAllTasks() {
        List<TaskResponseDTO> tasks = taskService.getAllTasks()
                .stream()
                .map(TaskResponseDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tasks);
    }

    /**
     * GET /api/tasks/{id} : Récupérer une tâche par ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(TaskResponseDTO::fromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/tasks : Créer une tâche avec assignation et notifications automatiques.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponseDTO> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        Long creatorId = extractUserId(authentication);
        log.info("Création d'une tâche par l'utilisateur ID : {}", creatorId);

        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .dueDate(request.getDueDate())
                .estimatedTime(request.getEstimatedTime())
                .spentTime(request.getSpentTime())
                .build();

        Task createdTask = taskService.createTask(
                task,
                creatorId,
                request.getProjectId(),
                request.getCategoryId(),
                request.getAssigneeIds()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponseDTO.fromEntity(createdTask));
    }

    /**
     * PATCH /api/tasks/{id}/status : Mettre à jour le statut d'une tâche (déclenche notification au créateur).
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TaskResponseDTO> updateTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            Authentication authentication
    ) {
        Long currentUserId = extractUserId(authentication);
        log.info("Mise à jour du statut de la tâche {} vers {} par utilisateur {}", id, request.getStatus(), currentUserId);

        Task updatedTask = taskService.updateTaskStatus(id, request.getStatus(), currentUserId);
        return ResponseEntity.ok(TaskResponseDTO.fromEntity(updatedTask));
    }

    /**
     * DELETE /api/tasks/{id} : Supprimer une tâche.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'email : " + email));
    }
}
