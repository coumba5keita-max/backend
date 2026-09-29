package com.coumba.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogTaskTimeRequest {

    /**
     * Temps estimé en heures (optionnel).
     */
    private Double estimatedTime;

    /**
     * Temps total passé en heures (remplace la valeur existante si spécifié).
     */
    private Double spentTime;

    /**
     * Temps additionnel à ajouter au temps déjà passé (ex : 1.5h aujourd'hui).
     */
    private Double additionalSpentTime;
}
