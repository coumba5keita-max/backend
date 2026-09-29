package com.coumba.dto.task;

import com.coumba.entities.enums.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityResponseDTO {

    private String code;       // HAUTE, MOYENNE, BASSE
    private String label;      // Haute, Moyenne, Basse
    private String color;      // #E53E3E, #D69E2E, #38A169
    private int level;         // 1 (urgente/haute), 2 (moyenne), 3 (basse)

    public static PriorityResponseDTO fromEnum(TaskPriority priority) {
        if (priority == null) return null;

        String label;
        String color;
        int level;

        switch (priority) {
            case HAUTE -> {
                label = "Haute";
                color = "#E53E3E";
                level = 1;
            }
            case MOYENNE -> {
                label = "Moyenne";
                color = "#D69E2E";
                level = 2;
            }
            case BASSE -> {
                label = "Basse";
                color = "#38A169";
                level = 3;
            }
            default -> {
                label = priority.name();
                color = "#718096";
                level = 4;
            }
        }

        return PriorityResponseDTO.builder()
                .code(priority.name())
                .label(label)
                .color(color)
                .level(level)
                .build();
    }
}
