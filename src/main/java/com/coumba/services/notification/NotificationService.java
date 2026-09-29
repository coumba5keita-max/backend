package com.coumba.services.notification;

import com.coumba.dto.notification.NotificationDTO;
import com.coumba.entities.notification.Notification;
import com.coumba.entities.task.Task;
import com.coumba.entities.user.User;
import com.coumba.entities.enums.TaskStatus;
import com.coumba.repositories.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    /**
     * Envoie une notification lors de l'assignation d'une tâche (BDD, WebSocket, Email).
     */
    @Transactional
    public void notifyTaskAssignment(User assignee, Task task, String assignedByName) {
        String message = String.format("La tâche \"%s\" vous a été assignée par %s.", task.getTitle(), assignedByName);

        // 1. Sauvegarde en Base de Données
        Notification notification = Notification.builder()
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .user(assignee)
                .task(task)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        // 2. Diffusion temps réel via WebSocket (STOMP)
        NotificationDTO dto = NotificationDTO.fromEntity(savedNotification);
        String destination = "/topic/user/" + assignee.getId() + "/notifications";
        messagingTemplate.convertAndSend(destination, dto);
        log.info("Notification WebSocket envoyée sur le canal {} : {}", destination, message);

        // 3. Envoi asynchrone d'un email de notification
        if (assignee.getEmail() != null && !assignee.getEmail().isBlank()) {
            emailService.sendTaskAssignmentEmail(assignee.getEmail(), task.getTitle(), assignedByName);
        }
    }

    /**
     * Envoie une notification lors de la mise à jour du statut d'une tâche (ex: passage à TERMINE).
     */
    @Transactional
    public void notifyTaskStatusChange(User recipient, Task task, TaskStatus newStatus, String updatedByName) {
        String message = String.format("Le statut de la tâche \"%s\" a changé : %s (mis à jour par %s).",
                task.getTitle(), newStatus.name(), updatedByName);

        // 1. Sauvegarde en Base de Données
        Notification notification = Notification.builder()
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .user(recipient)
                .task(task)
                .build();
        Notification savedNotification = notificationRepository.save(notification);

        // 2. Diffusion temps réel via WebSocket (STOMP)
        NotificationDTO dto = NotificationDTO.fromEntity(savedNotification);
        String destination = "/topic/user/" + recipient.getId() + "/notifications";
        messagingTemplate.convertAndSend(destination, dto);
        log.info("Notification de changement de statut envoyée sur le canal {} : {}", destination, message);

        // 3. Envoi asynchrone d'un email de notification
        if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            emailService.sendTaskStatusUpdateEmail(recipient.getEmail(), task.getTitle(), newStatus.name(), updatedByName);
        }
    }

    /**
     * Envoie une notification lors du changement de date d'échéance d'une tâche.
     */
    @Transactional
    public void notifyTaskDueDateChange(User recipient, Task task, LocalDate newDueDate, String adminName) {
        String formattedDate = newDueDate != null ? newDueDate.toString() : "Non définie";
        String message = String.format("L'échéance de la tâche \"%s\" a été modifiée au %s par %s.",
                task.getTitle(), formattedDate, adminName);

        Notification notification = Notification.builder()
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .user(recipient)
                .task(task)
                .build();
        Notification saved = notificationRepository.save(notification);

        NotificationDTO dto = NotificationDTO.fromEntity(saved);
        String destination = "/topic/user/" + recipient.getId() + "/notifications";
        messagingTemplate.convertAndSend(destination, dto);
        log.info("Notification d'échéance envoyée sur {} : {}", destination, message);

        if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            emailService.sendTaskDueDateChangeEmail(recipient.getEmail(), task.getTitle(), formattedDate, adminName);
        }
    }

    /**
     * Envoie une notification lors de la clôture définitive d'un ticket.
     */
    @Transactional
    public void notifyTicketClosed(User recipient, Task task, String adminName, String note) {
        String message = String.format("Le ticket \"%s\" a été définitivement clôturé par %s.",
                task.getTitle(), adminName);

        Notification notification = Notification.builder()
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .user(recipient)
                .task(task)
                .build();
        Notification saved = notificationRepository.save(notification);

        NotificationDTO dto = NotificationDTO.fromEntity(saved);
        String destination = "/topic/user/" + recipient.getId() + "/notifications";
        messagingTemplate.convertAndSend(destination, dto);
        log.info("Notification de clôture envoyée sur {} : {}", destination, message);

        if (recipient.getEmail() != null && !recipient.getEmail().isBlank()) {
            emailService.sendTicketClosedEmail(recipient.getEmail(), task.getTitle(), adminName, note);
        }
    }

    /**
     * Récupère l'historique des notifications pour un utilisateur donné.
     */
    @Transactional(readOnly = true)
    public List<NotificationDTO> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Marque une notification spécifique comme lue après vérification des droits.
     */
    @Transactional
    public NotificationDTO markAsRead(Long notificationId, Long currentUserId, boolean isAdmin) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable avec l'ID : " + notificationId));

        // Vérification de sécurité : seul le destinataire de la notification ou un administrateur peut la marquer comme lue
        if (!isAdmin && (notification.getUser() == null || !notification.getUser().getId().equals(currentUserId))) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier cette notification.");
        }

        notification.setIsRead(true);
        Notification updatedNotification = notificationRepository.save(notification);
        return NotificationDTO.fromEntity(updatedNotification);
    }
}
