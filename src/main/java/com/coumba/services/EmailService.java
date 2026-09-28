package com.coumba.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@taskmanager.com}")
    private String fromEmail;

    /**
     * Envoi d'un email asynchrone lors de l'assignation d'une tâche.
     *
     * @param to          Adresse email du destinataire
     * @param taskTitle   Titre de la tâche assignée
     * @param assignedBy  Nom de l'utilisateur qui a assigné la tâche
     */
    @Async
    public void sendTaskAssignmentEmail(String to, String taskTitle, String assignedBy) {
        log.info("Préparation de l'envoi d'email d'assignation à : {} pour la tâche : {}", to, taskTitle);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Nouvelle tâche assignée : " + taskTitle);
            message.setText(String.format(
                    "Bonjour,\n\n" +
                    "Une nouvelle tâche vous a été assignée par %s.\n\n" +
                    "Détails de la tâche :\n" +
                    "- Titre : %s\n\n" +
                    "Connectez-vous à la plateforme pour consulter tous les détails et commencer à y travailler.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe TaskManager",
                    assignedBy, taskTitle
            ));

            mailSender.send(message);
            log.info("Email d'assignation envoyé avec succès à : {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email d'assignation à {} : {}", to, e.getMessage());
        }
    }

    /**
     * Envoi d'un email asynchrone lors du changement de statut d'une tâche.
     *
     * @param to        Adresse email du destinataire (ex: créateur)
     * @param taskTitle Titre de la tâche
     * @param newStatus Nouveau statut de la tâche
     * @param updatedBy Nom de l'utilisateur ayant mis à jour la tâche
     */
    @Async
    public void sendTaskStatusUpdateEmail(String to, String taskTitle, String newStatus, String updatedBy) {
        log.info("Préparation de l'envoi d'email de mise à jour de statut à : {} pour la tâche : {}", to, taskTitle);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Mise à jour du statut de la tâche : " + taskTitle);
            message.setText(String.format(
                    "Bonjour,\n\n" +
                    "Le statut de votre tâche \"%s\" a été mis à jour par %s.\n\n" +
                    "Nouveau statut : %s\n\n" +
                    "Connectez-vous à la plateforme pour suivre l'avancement de votre projet.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe TaskManager",
                    taskTitle, updatedBy, newStatus
            ));

            mailSender.send(message);
            log.info("Email de mise à jour envoyé avec succès à : {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de mise à jour à {} : {}", to, e.getMessage());
        }
    }
}
