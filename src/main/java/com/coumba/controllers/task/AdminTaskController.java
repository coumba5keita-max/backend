package com.coumba.controllers.task;

import com.coumba.dto.task.*;
import com.coumba.entities.enums.TaskPriority;
import com.coumba.entities.enums.TaskStatus;
import com.coumba.services.task.AdminTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tasks")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AdminTaskController {

    private final AdminTaskService adminTaskService;

    /**
     * POST /api/admin/tasks : Créer une tâche et l'attribuer à un ou plusieurs collaborateurs (ex : Aboubacar ou l'admin lui-même).
     */
    @PostMapping
    public ResponseEntity<TaskResponseDTO> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        TaskResponseDTO created = adminTaskService.createTask(request, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/admin/tasks/{id}/reassign : Réaffecter une tâche d'un membre à un ou plusieurs autres collaborateurs.
     */
    @PutMapping("/{id}/reassign")
    public ResponseEntity<TaskResponseDTO> reassignTask(
            @PathVariable Long id,
            @Valid @RequestBody ReassignTaskRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        TaskResponseDTO updated = adminTaskService.reassignTask(id, request, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/admin/tasks/{id}/due-date : Modifier l'échéance (Due date) d'une tâche.
     */
    @PatchMapping("/{id}/due-date")
    public ResponseEntity<TaskResponseDTO> updateDueDate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDueDateRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        TaskResponseDTO updated = adminTaskService.updateDueDate(id, request, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/admin/tasks/{id}/close : Clôturer définitivement un ticket (statut CLOTURE + date de clôture).
     */
    @PatchMapping("/{id}/close")
    public ResponseEntity<TaskResponseDTO> closeTicket(
            @PathVariable Long id,
            @RequestBody(required = false) CloseTicketRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        TaskResponseDTO updated = adminTaskService.closeTicket(id, request, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * GET /api/admin/tasks : Consulter les tâches avec filtres (statut, priorité, projet, collaborateur assigné, en retard).
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getAllAdminTasks(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Boolean overdue
    ) {
        return ResponseEntity.ok(adminTaskService.getAllAdminTasks(status, priority, projectId, assigneeId, overdue));
    }
}
