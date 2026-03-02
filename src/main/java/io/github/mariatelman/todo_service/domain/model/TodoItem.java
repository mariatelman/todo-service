package io.github.mariatelman.todo_service.domain.model;

import io.github.mariatelman.todo_service.domain.exceptions.TodoModificationNotAllowedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.Instant;
import java.util.Objects;

@Entity
public class TodoItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(nullable = false)
  private String description;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TodoStatus status;
  @Column(nullable = false, updatable = false)
  private Instant createdAt;
  private Instant dueAt;
  private Instant doneAt;

  public TodoItem() {
  }

  public TodoItem(final String description, final Instant createdAt, final Instant dueAt, final TodoStatus status) {
    validateDescription(description);
    this.description  = description;
    this.dueAt = dueAt;
    this.createdAt = createdAt;
    this.status = status;
  }

  public Long id() {
    return id;
  }

  public String description() {
    return description;
  }

  public void setDescription(final String description) {
    ensureMutable();
    validateDescription(description);
    this.description = description;
  }

  private void validateDescription(final String description) {
    if (description == null || description.isBlank()) {
      throw new IllegalArgumentException("Description must not be empty");
    }
  }

  public TodoStatus status() {
    return status;
  }

  public void setNotDone(final Instant instant) {
    ensureMutable();
    if (dueDateBefore(instant)) {
      throw new TodoModificationNotAllowedException(this);
    }
    this.status = TodoStatus.NOT_DONE;
    this.doneAt = null;
  }

  public void setDone(final Instant doneAt) {
    ensureMutable();
    if (dueDateBefore(doneAt)) {
      throw new TodoModificationNotAllowedException(this);
    }
    if (status != TodoStatus.DONE) {
      this.status = TodoStatus.DONE;
      this.doneAt = doneAt;
    }
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant dueAt() {
    return dueAt;
  }

  public Instant doneAt() {
    return doneAt;
  }

  public boolean isPastDue() {
    return TodoStatus.PAST_DUE == status;
  }

  public TodoStatus effectiveStatus(final Instant nowInstant) {
    if (isEffectivePastDue(nowInstant)) {
      return TodoStatus.PAST_DUE;
    } else {
      return status;
    }
  }

  public boolean refreshPastDue(final Instant nowInstant) {
    if (isEffectivePastDue(nowInstant)) {
      status = TodoStatus.PAST_DUE;
      return true;
    }
    return false;
  }

  private boolean isEffectivePastDue(final Instant nowInstant) {
    return status == TodoStatus.NOT_DONE && dueDateBefore(nowInstant);
  }

  public boolean dueDateBefore(final Instant instant) {
    return dueAt != null && dueAt.isBefore(instant);
  }

  private void ensureMutable() {
    if (isPastDue()) {
      throw new TodoModificationNotAllowedException(this);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final TodoItem todoItem = (TodoItem) o;
    return Objects.equals(id, todoItem.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return "TodoItem{" +
      "id=" + id +
      ", description='" + description + '\'' +
      ", status=" + status +
      ", createdAt=" + createdAt +
      ", dueAt=" + dueAt +
      ", doneAt=" + doneAt +
      '}';
  }

}
