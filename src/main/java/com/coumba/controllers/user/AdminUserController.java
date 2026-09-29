package com.coumba.controllers.user;

import com.coumba.dto.user.InviteUserRequest;
import com.coumba.dto.user.ToggleUserStatusRequest;
import com.coumba.dto.user.UpdateUserRequest;
import com.coumba.dto.user.UserResponseDTO;
import com.coumba.services.user.AdminUserService;
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
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * GET /api/admin/users : Récupère la liste de tous les utilisateurs.
     */
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    /**
     * GET /api/admin/users/{id} : Récupère les détails d'un utilisateur.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    /**
     * POST /api/admin/users/invite : Inviter un nouvel utilisateur.
     * Génère un mot de passe temporaire cryptographique et envoie un email d'invitation.
     */
    @PostMapping("/invite")
    public ResponseEntity<UserResponseDTO> inviteUser(
            @Valid @RequestBody InviteUserRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        UserResponseDTO user = adminUserService.inviteUser(request, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * PUT /api/admin/users/{id} : Modifier un compte utilisateur (nom, prénom, email, rôle).
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        UserResponseDTO updated = adminUserService.updateUser(id, request, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * PATCH /api/admin/users/{id}/status : Activer ou désactiver un compte utilisateur.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDTO> toggleUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody ToggleUserStatusRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        UserResponseDTO updated = adminUserService.toggleUserStatus(id, request.getActive(), adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/admin/users/{userId}/teams/{teamId} : Assigner un utilisateur à une équipe.
     */
    @PostMapping("/{userId}/teams/{teamId}")
    public ResponseEntity<UserResponseDTO> assignUserToTeam(
            @PathVariable Long userId,
            @PathVariable Long teamId,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        UserResponseDTO updated = adminUserService.assignUserToTeam(userId, teamId, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/admin/users/{userId}/teams/{teamId} : Retirer un utilisateur d'une équipe.
     */
    @DeleteMapping("/{userId}/teams/{teamId}")
    public ResponseEntity<UserResponseDTO> removeUserFromTeam(
            @PathVariable Long userId,
            @PathVariable Long teamId,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        UserResponseDTO updated = adminUserService.removeUserFromTeam(userId, teamId, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/admin/users/{userId}/projects/{projectId} : Assigner un utilisateur à un projet.
     */
    @PostMapping("/{userId}/projects/{projectId}")
    public ResponseEntity<UserResponseDTO> assignUserToProject(
            @PathVariable Long userId,
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        UserResponseDTO updated = adminUserService.assignUserToProject(userId, projectId, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/admin/users/{userId}/projects/{projectId} : Retirer un utilisateur d'un projet.
     */
    @DeleteMapping("/{userId}/projects/{projectId}")
    public ResponseEntity<UserResponseDTO> removeUserFromProject(
            @PathVariable Long userId,
            @PathVariable Long projectId,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        UserResponseDTO updated = adminUserService.removeUserFromProject(userId, projectId, adminEmail);
        return ResponseEntity.ok(updated);
    }
}
