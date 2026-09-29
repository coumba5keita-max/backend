package com.coumba.services.task;

import com.coumba.dto.task.AttachmentResponseDTO;
import com.coumba.dto.task.CommentResponseDTO;
import com.coumba.dto.task.CreateCommentRequest;
import com.coumba.entities.task.Attachment;
import com.coumba.entities.task.Comment;
import com.coumba.entities.task.Task;
import com.coumba.entities.user.User;
import com.coumba.exceptions.task.TaskNotFoundException;
import com.coumba.exceptions.user.UserNotFoundException;
import com.coumba.repositories.task.AttachmentRepository;
import com.coumba.repositories.task.CommentRepository;
import com.coumba.repositories.task.TaskRepository;
import com.coumba.repositories.user.UserRepository;
import com.coumba.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskInteractionService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final NotificationService notificationService;

    private static final String UPLOAD_DIR = "uploads/attachments";

    /**
     * Poste un commentaire sur une tâche (pour demander une précision, partager un avancement, etc.).
     */
    @Transactional
    public CommentResponseDTO addComment(Long taskId, CreateCommentRequest request, String userEmail) {
        log.info("Ajout d'un commentaire sur la tâche ID [{}] par [{}]", taskId, userEmail);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        User author = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userEmail));

        Comment comment = Comment.builder()
                .content(request.getContent().trim())
                .createdAt(LocalDateTime.now())
                .task(task)
                .user(author)
                .build();

        Comment saved = commentRepository.save(comment);

        // Notifier les autres collaborateurs et le créateur
        Set<User> recipients = new HashSet<>();
        if (task.getAssignees() != null) recipients.addAll(task.getAssignees());
        if (task.getCreator() != null) recipients.add(task.getCreator());
        recipients.remove(author); // Ne pas s'auto-notifier

        String authorFullName = author.getFirstname() + " " + author.getLastname();
        for (User recipient : recipients) {
            notificationService.notifyTaskComment(recipient, task, authorFullName, request.getContent());
        }

        log.info("Commentaire ID [{}] ajouté avec succès", saved.getId());
        return CommentResponseDTO.fromEntity(saved);
    }

    /**
     * Récupère la liste de tous les commentaires d'une tâche ordonnés par date.
     */
    @Transactional(readOnly = true)
    public List<CommentResponseDTO> getComments(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }

        return commentRepository.findByTaskId(taskId).stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .map(CommentResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Téléverse une pièce jointe ou une capture d'écran sur une tâche.
     */
    @Transactional
    public AttachmentResponseDTO addAttachment(Long taskId, MultipartFile file, String userEmail) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier téléversé ne peut pas être vide.");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        User uploader = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable : " + userEmail));

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "fichier";
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalName.substring(dotIndex);
        }

        String storedFileName = UUID.randomUUID().toString() + extension;

        try {
            Path uploadPath = getUploadPath();
            Path targetPath = uploadPath.resolve(storedFileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            Attachment attachment = Attachment.builder()
                    .fileName(originalName)
                    .fileUrl(targetPath.toString())
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .uploadedAt(LocalDateTime.now())
                    .task(task)
                    .user(uploader)
                    .build();

            Attachment saved = attachmentRepository.save(attachment);

            // Notifier le créateur et les autres assignés
            Set<User> recipients = new HashSet<>();
            if (task.getAssignees() != null) recipients.addAll(task.getAssignees());
            if (task.getCreator() != null) recipients.add(task.getCreator());
            recipients.remove(uploader);

            String uploaderFullName = uploader.getFirstname() + " " + uploader.getLastname();
            for (User recipient : recipients) {
                notificationService.notifyTaskAttachment(recipient, task, uploaderFullName, originalName);
            }

            log.info("Pièce jointe [{}] enregistrée avec succès pour la tâche ID [{}]", originalName, taskId);
            return AttachmentResponseDTO.fromEntity(saved);

        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement du fichier : {}", e.getMessage());
            throw new RuntimeException("Échec de l'enregistrement de la pièce jointe : " + e.getMessage(), e);
        }
    }

    /**
     * Récupère la liste des pièces jointes et captures d'écran liées à une tâche.
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponseDTO> getAttachments(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException(taskId);
        }

        return attachmentRepository.findByTaskId(taskId).stream()
                .sorted(Comparator.comparing(Attachment::getUploadedAt).reversed())
                .map(AttachmentResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Téléchargement d'un fichier joint par son ID.
     */
    @Transactional(readOnly = true)
    public AttachmentFile getAttachmentFile(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Pièce jointe introuvable avec l'ID : " + attachmentId));

        try {
            Path filePath = Paths.get(attachment.getFileUrl());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalArgumentException("Le fichier physique est introuvable sur le serveur.");
            }

            return new AttachmentFile(resource, attachment.getFileName(), attachment.getFileType());

        } catch (MalformedURLException e) {
            throw new RuntimeException("Erreur lors de la lecture du fichier : " + e.getMessage(), e);
        }
    }

    private Path getUploadPath() throws IOException {
        Path uploadPath = Paths.get(UPLOAD_DIR);
        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            return uploadPath;
        } catch (Exception e) {
            log.warn("Impossible de créer dans ./uploads/attachments, utilisation du répertoire temporaire : {}", e.getMessage());
            Path fallback = Paths.get(System.getProperty("java.io.tmpdir"), "taskmanager_uploads");
            if (!Files.exists(fallback)) {
                Files.createDirectories(fallback);
            }
            return fallback;
        }
    }

    public record AttachmentFile(Resource resource, String fileName, String contentType) {}
}
