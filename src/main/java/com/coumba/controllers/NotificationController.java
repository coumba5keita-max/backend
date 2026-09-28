package com.coumba.controllers;

import com.coumba.dto.NotificationDTO;
import com.coumba.entities.User;
import com.coumba.repositories.UserRepository;
import com.coumba.security.CustomUserDetails;
import com.coumba.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * GET /api/notifications : Récupère l'historique des notifications de l'utilisateur connecté.
     * Accessible aux utilisateurs ayant le rôle ROLE_USER ou ROLE_ADMIN.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<NotificationDTO>> getMyNotifications(Authentication authentication) {
        Long currentUserId = extractUserId(authentication);
        log.info("Récupération des notifications pour l'utilisateur ID : {}", currentUserId);
        List<NotificationDTO> notifications = notificationService.getUserNotifications(currentUserId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * PATCH /api/notifications/{id}/read : Marque une notification comme lue.
     * Accessible aux utilisateurs ayant le rôle ROLE_USER ou ROLE_ADMIN.
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<NotificationDTO> markAsRead(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long currentUserId = extractUserId(authentication);
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        log.info("Marquage de la notification ID {} comme lue par l'utilisateur ID {} (Admin: {})", id, currentUserId, isAdmin);
        NotificationDTO updated = notificationService.markAsRead(id, currentUserId, isAdmin);
        return ResponseEntity.ok(updated);
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }

        // Fallback par email
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'email : " + email));
    }
}
