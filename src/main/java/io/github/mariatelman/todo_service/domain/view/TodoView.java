package io.github.mariatelman.todo_service.domain.view;

import io.github.mariatelman.todo_service.domain.model.TodoItem;
import io.github.mariatelman.todo_service.domain.model.TodoStatus;

import java.time.Instant;

public record TodoView(Long id, String description, String status, Instant createdAt, Instant dueAt, Instant doneAt) {

  public TodoView(final TodoItem item, final TodoStatus effectiveStatus) {
    this(item.id(), item.description(), effectiveStatus.name(), item.createdAt(), item.dueAt(), item.doneAt());
  }
}
