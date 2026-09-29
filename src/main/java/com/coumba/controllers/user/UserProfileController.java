package com.coumba.controllers.user;

import com.coumba.dto.user.ChangePasswordRequest;
import com.coumba.dto.user.UpdateProfileRequest;
import com.coumba.dto.user.UserProfileResponseDTO;
import com.coumba.services.user.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user/profile")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * GET /api/user/profile : Consulter les informations de son profil.
     */
    @GetMapping
    public ResponseEntity<UserProfileResponseDTO> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(userProfileService.getProfile(email));
    }

    /**
     * PUT /api/user/profile : Mettre à jour ses informations personnelles (prénom, nom, email).
     */
    @PutMapping
    public ResponseEntity<UserProfileResponseDTO> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication
    ) {
        String currentEmail = authentication.getName();
        return ResponseEntity.ok(userProfileService.updateProfile(currentEmail, request));
    }

    /**
     * PUT /api/user/profile/password : Mettre à jour son mot de passe en fournissant l'ancien.
     */
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        userProfileService.changePassword(email, request);
        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
    }
}
