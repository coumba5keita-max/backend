package com.coumba.dto.task;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReassignTaskRequest {

    @NotEmpty(message = "La liste des identifiants des nouveaux collaborateurs assignés est obligatoire")
    private Set<Long> assigneeIds;

    private String reason;
}
