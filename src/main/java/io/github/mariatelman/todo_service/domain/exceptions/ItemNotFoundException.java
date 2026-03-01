package io.github.mariatelman.todo_service.domain.exceptions;

public class ItemNotFoundException extends RuntimeException {

  public ItemNotFoundException(final Long id) {
    super("Item with id " + id + " not found");
  }
}
