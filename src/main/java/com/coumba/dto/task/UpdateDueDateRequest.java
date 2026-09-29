package com.coumba.dto.task;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateDueDateRequest {

    @NotNull(message = "La date d'échéance (dueDate) est obligatoire")
    private LocalDate dueDate;
}
