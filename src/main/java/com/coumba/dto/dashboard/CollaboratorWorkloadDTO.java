package com.coumba.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollaboratorWorkloadDTO {

    private Long userId;
    private String fullName;
    private String email;
    private String role;
    private long assignedTasksCount;
    private long inProgressTasksCount;
    private long overdueTasksCount;
    private long completedTasksCount;
    private double workloadPercentage;
}
