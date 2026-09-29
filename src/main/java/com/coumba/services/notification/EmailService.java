package com.coumba.services.notification;

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

    /**
     * Envoi d'un email d'invitation avec mot de passe temporaire généré de façon sécurisée.
     */
    @Async
    public void sendUserInvitationEmail(String to, String temporaryPassword, String invitedBy) {
        log.info("Envoi d'un email d'invitation à : {} par l'administrateur : {}", to, invitedBy);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Bienvenue sur TaskManager - Vos accès de connexion");
            message.setText(String.format(
                    "Bonjour,\n\n" +
                    "Vous avez été invité sur la plateforme TaskManager par le responsable %s.\n\n" +
                    "Voici vos identifiants temporaires de connexion :\n" +
                    "- Identifiant (Email) : %s\n" +
                    "- Mot de passe temporaire : %s\n\n" +
                    "CONSIGNE DE SÉCURITÉ :\n" +
                    "Pour la sécurité de votre compte, veuillez modifier ce mot de passe temporaire dès votre première connexion.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe TaskManager",
                    invitedBy, to, temporaryPassword
            ));

            mailSender.send(message);
            log.info("Email d'invitation envoyé avec succès à : {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email d'invitation à {} : {}", to, e.getMessage());
        }
    }

    /**
     * Envoi d'un email d'information lors du changement de statut d'un compte (activé / désactivé).
     */
    @Async
    public void sendAccountStatusChangeEmail(String to, boolean isActive, String updatedBy) {
        log.info("Envoi d'un email de notification de statut à : {} (Actif : {})", to, isActive);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            String statusText = isActive ? "réactivé" : "désactivé";
            message.setSubject("Information importante - Votre compte TaskManager a été " + statusText);
            message.setText(String.format(
                    "Bonjour,\n\n" +
                    "Votre compte sur la plateforme TaskManager a été %s par l'administrateur %s.\n\n" +
                    (isActive
                        ? "Vous pouvez désormais vous reconnecter normalement à vos projets et équipes."
                        : "Votre accès est suspendu. Pour toute question, veuillez vous rapprocher de votre responsable d'équipe.") +
                    "\n\nCordialement,\n" +
                    "L'équipe TaskManager",
                    statusText, updatedBy
            ));

            mailSender.send(message);
            log.info("Email de changement de statut envoyé avec succès à : {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de statut à {} : {}", to, e.getMessage());
        }
    }

    /**
     * Envoi d'un email asynchrone lors de la modification de la date d'échéance d'une tâche.
     */
    @Async
    public void sendTaskDueDateChangeEmail(String to, String taskTitle, String newDueDate, String updatedBy) {
        log.info("Préparation de l'envoi d'email de changement d'échéance à : {} pour la tâche : {}", to, taskTitle);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Mise à jour d'échéance pour la tâche : " + taskTitle);
            message.setText(String.format(
                    "Bonjour,\n\n" +
                    "La date d'échéance de votre tâche \"%s\" a été modifiée par l'administrateur %s.\n\n" +
                    "Nouvelle échéance : %s\n\n" +
                    "Veuillez vous connecter à la plateforme pour organiser votre travail en conséquence.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe TaskManager",
                    taskTitle, updatedBy, newDueDate
            ));

            mailSender.send(message);
            log.info("Email de changement d'échéance envoyé avec succès à : {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de changement d'échéance à {} : {}", to, e.getMessage());
        }
    }

    /**
     * Envoi d'un email asynchrone lors de la clôture définitive d'un ticket.
     */
    @Async
    public void sendTicketClosedEmail(String to, String taskTitle, String closedBy, String note) {
        log.info("Préparation de l'envoi d'email de clôture de ticket à : {} pour la tâche : {}", to, taskTitle);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Ticket clôturé définitivement : " + taskTitle);
            message.setText(String.format(
                    "Bonjour,\n\n" +
                    "Le ticket \"%s\" a été définitivement clôturé par l'administrateur %s.\n\n" +
                    (note != null && !note.isBlank() ? "Note de clôture : " + note + "\n\n" : "") +
                    "Ce ticket est désormais archivé au statut CLÔTURÉ.\n\n" +
                    "Cordialement,\n" +
                    "L'équipe TaskManager",
                    taskTitle, closedBy
            ));

            mailSender.send(message);
            log.info("Email de clôture envoyé avec succès à : {}", to);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'email de clôture à {} : {}", to, e.getMessage());
        }
    }
}
