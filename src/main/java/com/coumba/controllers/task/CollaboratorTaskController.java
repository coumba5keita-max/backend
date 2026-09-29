package com.coumba.controllers.task;

import com.coumba.dto.task.LogTaskTimeRequest;
import com.coumba.dto.task.TaskResponseDTO;
import com.coumba.dto.task.UpdateTaskStatusRequest;
import com.coumba.entities.enums.TaskPriority;
import com.coumba.entities.enums.TaskStatus;
import com.coumba.services.task.CollaboratorTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/tasks")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CollaboratorTaskController {

    private final CollaboratorTaskService collaboratorTaskService;

    /**
     * GET /api/user/tasks : Filtrer et rechercher les tâches attribuées à l'utilisateur connecté.
     */
    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getMyTasks(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean overdue,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(collaboratorTaskService.getMyTasks(
                email, search, status, priority, projectId, categoryId, overdue
        ));
    }

    /**
     * PATCH /api/user/tasks/{id}/status : Changer l'état d'avancement d'une tâche assignée (A_FAIRE -> EN_COURS -> TERMINE).
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponseDTO> updateMyTaskStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskStatusRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(collaboratorTaskService.updateMyTaskStatus(id, request.getStatus(), email));
    }

    /**
     * PATCH /api/user/tasks/{id}/time : Estimer ou saisir le temps passé sur un ticket.
     */
    @PatchMapping("/{id}/time")
    public ResponseEntity<TaskResponseDTO> logTaskTime(
            @PathVariable Long id,
            @RequestBody LogTaskTimeRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(collaboratorTaskService.logTaskTime(id, request, email));
    }
}
