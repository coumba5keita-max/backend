package com.coumba.dto.task;

import com.coumba.entities.task.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String color;
    private int taskCount;

    public static CategoryResponseDTO fromEntity(Category category) {
        if (category == null) return null;

        int count = category.getTasks() != null ? category.getTasks().size() : 0;

        return CategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .color(category.getColor())
                .taskCount(count)
                .build();
    }
}
