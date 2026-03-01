package io.github.mariatelman.todo_service.domain.exceptions;

public class TodoModificationNotAllowedException extends RuntimeException {

  public TodoModificationNotAllowedException(final String message) {
    super(message);
  }

}
