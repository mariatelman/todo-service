package io.github.mariatelman.todo_service.api.dto.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;


import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CreateTodoItemRequest(@NotBlank String description, Instant dueAt) {
}
