package com.coumba.controllers.project;

import com.coumba.dto.team.AssignUsersRequest;
import com.coumba.dto.user.UserResponseDTO;
import com.coumba.services.project.AdminProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/projects")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AdminProjectController {

    private final AdminProjectService adminProjectService;

    /**
     * GET /api/admin/projects/{projectId}/members : Lister les membres d'un projet.
     */
    @GetMapping("/{projectId}/members")
    public ResponseEntity<List<UserResponseDTO>> getProjectMembers(@PathVariable Long projectId) {
        return ResponseEntity.ok(adminProjectService.getProjectMembers(projectId));
    }

    /**
     * POST /api/admin/projects/{projectId}/users/{userId} : Assigner un utilisateur à un projet spécifique.
     */
    @PostMapping("/{projectId}/users/{userId}")
    public ResponseEntity<Map<String, String>> addUserToProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        adminProjectService.addUserToProject(projectId, userId, adminEmail);
        return ResponseEntity.ok(Map.of("message", "Utilisateur ID " + userId + " assigné avec succès au projet ID " + projectId));
    }

    /**
     * DELETE /api/admin/projects/{projectId}/users/{userId} : Retirer un utilisateur d'un projet.
     */
    @DeleteMapping("/{projectId}/users/{userId}")
    public ResponseEntity<Map<String, String>> removeUserFromProject(
            @PathVariable Long projectId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        adminProjectService.removeUserFromProject(projectId, userId, adminEmail);
        return ResponseEntity.ok(Map.of("message", "Utilisateur ID " + userId + " retiré avec succès du projet ID " + projectId));
    }

    /**
     * POST /api/admin/projects/{projectId}/members : Définir en bloc les utilisateurs affectés à un projet.
     */
    @PostMapping("/{projectId}/members")
    public ResponseEntity<Map<String, Object>> setProjectMembers(
            @PathVariable Long projectId,
            @Valid @RequestBody AssignUsersRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        adminProjectService.setProjectMembers(projectId, request.getUserIds(), adminEmail);
        return ResponseEntity.ok(Map.of(
                "message", "Membres du projet ID " + projectId + " mis à jour avec succès",
                "assignedUserCount", request.getUserIds().size()
        ));
    }
}
