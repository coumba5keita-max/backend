package com.coumba.dto;

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
public class AssignUsersRequest {

    @NotEmpty(message = "La liste des identifiants utilisateurs ne peut pas être vide")
    private Set<Long> userIds;
}
