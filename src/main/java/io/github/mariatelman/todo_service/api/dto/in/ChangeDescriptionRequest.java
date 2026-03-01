package io.github.mariatelman.todo_service.api.dto.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChangeDescriptionRequest(@NotBlank String description) {
}
