package com.coumba.services.user;

import com.coumba.dto.user.ChangePasswordRequest;
import com.coumba.dto.user.UpdateProfileRequest;
import com.coumba.dto.user.UserProfileResponseDTO;
import com.coumba.entities.user.User;
import com.coumba.exceptions.user.UserNotFoundException;
import com.coumba.repositories.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Récupère le profil complet de l'utilisateur actuellement authentifié.
     */
    @Transactional(readOnly = true)
    public UserProfileResponseDTO getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec l'email : " + email));

        return UserProfileResponseDTO.fromEntity(user);
    }

    /**
     * Met à jour les informations personnelles du collaborateur (prénom, nom, email éventuel).
     */
    @Transactional
    public UserProfileResponseDTO updateProfile(String currentEmail, UpdateProfileRequest request) {
        log.info("Mise à jour du profil pour l'utilisateur [{}]", currentEmail);

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec l'email : " + currentEmail));

        // Vérification si modification de l'adresse email
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new IllegalArgumentException("L'adresse email \"" + newEmail + "\" est déjà utilisée par un autre compte.");
            }
            user.setEmail(newEmail);
        }

        user.setFirstname(request.getFirstname().trim());
        user.setLastname(request.getLastname().trim());

        User saved = userRepository.save(user);
        log.info("Profil de l'utilisateur [{}] mis à jour avec succès", saved.getEmail());
        return UserProfileResponseDTO.fromEntity(saved);
    }

    /**
     * Modifie de façon sécurisée le mot de passe du collaborateur après vérification de l'ancien.
     */
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        log.info("Demande de changement de mot de passe pour [{}]", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable avec l'email : " + email));

        // 1. Vérification que l'ancien mot de passe fourni est correct
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Le mot de passe actuel renseigné est incorrect.");
        }

        // 2. Vérification que le nouveau mot de passe est différent
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Le nouveau mot de passe doit être différent de l'ancien.");
        }

        // 3. Chiffrement et enregistrement
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Mot de passe mis à jour avec succès pour [{}]", email);
    }
}
