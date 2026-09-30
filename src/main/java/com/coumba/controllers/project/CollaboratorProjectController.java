package com.coumba.controllers.project;

import com.coumba.dto.project.ProjectResponseDTO;
import com.coumba.services.project.AdminProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class CollaboratorProjectController {

    private final AdminProjectService adminProjectService;

    /**
     * GET /api/user/projects : Récupère la liste des projets assignés à l'utilisateur connecté (ou tous les projets actifs pour un admin).
     * Accessible à ROLE_USER et ROLE_ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<ProjectResponseDTO>> getMyProjects(Authentication authentication) {
        String userEmail = authentication.getName();
        log.info("L'utilisateur [{}] consulte ses projets assignés", userEmail);
        List<ProjectResponseDTO> projects = adminProjectService.getUserProjects(userEmail);
        return ResponseEntity.ok(projects);
    }
}
