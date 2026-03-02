package io.github.mariatelman.todo_service.domain.exceptions;

import io.github.mariatelman.todo_service.domain.model.TodoItem;

public class TodoModificationNotAllowedException extends RuntimeException {

  public TodoModificationNotAllowedException(final TodoItem item) {
    super("Not allowed to modify a past due item: " + item);
  }

}
