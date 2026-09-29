package com.coumba.controllers.task;

import com.coumba.dto.task.AttachmentResponseDTO;
import com.coumba.dto.task.CommentResponseDTO;
import com.coumba.dto.task.CreateCommentRequest;
import com.coumba.services.task.TaskInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class TaskInteractionController {

    private final TaskInteractionService taskInteractionService;

    // --- COMMENTAIRES ---

    /**
     * POST /api/tasks/{taskId}/comments : Poster un commentaire sur une tâche pour demander une précision.
     */
    @PostMapping("/api/tasks/{taskId}/comments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<CommentResponseDTO> addComment(
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        CommentResponseDTO created = taskInteractionService.addComment(taskId, request, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/tasks/{taskId}/comments : Récupérer la liste des commentaires d'une tâche.
     */
    @GetMapping("/api/tasks/{taskId}/comments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<CommentResponseDTO>> getComments(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskInteractionService.getComments(taskId));
    }

    // --- PIÈCES JOINTES & CAPTURES D'ÉCRAN ---

    /**
     * POST /api/tasks/{taskId}/attachments : Joindre des fichiers ou des captures d'écran à un ticket.
     */
    @PostMapping(value = "/api/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<AttachmentResponseDTO> addAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        String email = authentication.getName();
        AttachmentResponseDTO created = taskInteractionService.addAttachment(taskId, file, email);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/tasks/{taskId}/attachments : Lister les pièces jointes et captures d'écran d'un ticket.
     */
    @GetMapping("/api/tasks/{taskId}/attachments")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<AttachmentResponseDTO>> getAttachments(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskInteractionService.getAttachments(taskId));
    }

    /**
     * GET /api/attachments/{id}/download : Télécharger une pièce jointe ou capture d'écran.
     */
    @GetMapping("/api/attachments/{id}/download")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        TaskInteractionService.AttachmentFile fileData = taskInteractionService.getAttachmentFile(id);

        MediaType mediaType;
        try {
            mediaType = fileData.contentType() != null
                    ? MediaType.parseMediaType(fileData.contentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileData.fileName() + "\"")
                .body(fileData.resource());
    }

    /**
     * GET /api/attachments/{id} : Visualiser un fichier en ligne (ex: capture d'écran inline).
     */
    @GetMapping("/api/attachments/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Resource> viewAttachment(@PathVariable Long id) {
        TaskInteractionService.AttachmentFile fileData = taskInteractionService.getAttachmentFile(id);

        MediaType mediaType;
        try {
            mediaType = fileData.contentType() != null
                    ? MediaType.parseMediaType(fileData.contentType())
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileData.fileName() + "\"")
                .body(fileData.resource());
    }
}
