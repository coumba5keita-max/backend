package com.coumba.dto.project;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchiveProjectRequest {

    @NotNull(message = "Le statut d'archivage (true/false) est obligatoire")
    private Boolean isArchived;
}
