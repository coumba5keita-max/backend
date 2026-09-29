package com.coumba.controllers.team;

import com.coumba.dto.team.AssignUsersRequest;
import com.coumba.dto.team.TeamRequest;
import com.coumba.dto.team.TeamResponseDTO;
import com.coumba.dto.user.UserResponseDTO;
import com.coumba.services.team.AdminTeamService;
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
@RequestMapping("/api/admin/teams")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AdminTeamController {

    private final AdminTeamService adminTeamService;

    /**
     * GET /api/admin/teams : Liste toutes les équipes.
     */
    @GetMapping
    public ResponseEntity<List<TeamResponseDTO>> getAllTeams() {
        return ResponseEntity.ok(adminTeamService.getAllTeams());
    }

    /**
     * GET /api/admin/teams/{id} : Détails d'une équipe.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TeamResponseDTO> getTeamById(@PathVariable Long id) {
        return ResponseEntity.ok(adminTeamService.getTeamById(id));
    }

    /**
     * GET /api/admin/teams/{id}/users : Liste les membres d'une équipe.
     */
    @GetMapping("/{id}/users")
    public ResponseEntity<List<UserResponseDTO>> getTeamMembers(@PathVariable Long id) {
        return ResponseEntity.ok(adminTeamService.getTeamMembers(id));
    }

    /**
     * POST /api/admin/teams : Créer une nouvelle équipe.
     */
    @PostMapping
    public ResponseEntity<TeamResponseDTO> createTeam(
            @Valid @RequestBody TeamRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        TeamResponseDTO team = adminTeamService.createTeam(request, adminEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(team);
    }

    /**
     * PUT /api/admin/teams/{id} : Modifier une équipe.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TeamResponseDTO> updateTeam(
            @PathVariable Long id,
            @Valid @RequestBody TeamRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        TeamResponseDTO updated = adminTeamService.updateTeam(id, request, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/admin/teams/{id} : Supprimer une équipe.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        adminTeamService.deleteTeam(id, adminEmail);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/admin/teams/{teamId}/users/{userId} : Assigner un utilisateur à une équipe spécifique.
     */
    @PostMapping("/{teamId}/users/{userId}")
    public ResponseEntity<TeamResponseDTO> addUserToTeam(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        TeamResponseDTO updated = adminTeamService.addUserToTeam(teamId, userId, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/admin/teams/{teamId}/users/{userId} : Retirer un utilisateur d'une équipe spécifique.
     */
    @DeleteMapping("/{teamId}/users/{userId}")
    public ResponseEntity<TeamResponseDTO> removeUserFromTeam(
            @PathVariable Long teamId,
            @PathVariable Long userId,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        TeamResponseDTO updated = adminTeamService.removeUserFromTeam(teamId, userId, adminEmail);
        return ResponseEntity.ok(updated);
    }

    /**
     * POST /api/admin/teams/{teamId}/members : Définir en bloc les membres d'une équipe.
     */
    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamResponseDTO> setTeamMembers(
            @PathVariable Long teamId,
            @Valid @RequestBody AssignUsersRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : "ADMIN";
        TeamResponseDTO updated = adminTeamService.setTeamMembers(teamId, request.getUserIds(), adminEmail);
        return ResponseEntity.ok(updated);
    }
}
