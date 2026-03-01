package io.github.mariatelman.todo_service.application;

import io.github.mariatelman.todo_service.domain.model.TodoItem;
import io.github.mariatelman.todo_service.domain.model.TodoStatus;
import io.github.mariatelman.todo_service.domain.exceptions.ItemNotFoundException;
import io.github.mariatelman.todo_service.domain.view.TodoView;
import io.github.mariatelman.todo_service.domain.view.TodoViewList;
import io.github.mariatelman.todo_service.repo.TodoRepository;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class TodoService {

  private static final Logger logger = LoggerFactory.getLogger(TodoService.class);
  private final TodoRepository toDoRepository;
  private final Clock clock;

  public TodoService(final TodoRepository toDoRepository, final Clock clock) {
    this.toDoRepository = toDoRepository;
    this.clock = clock;
  }

  @Transactional
  public TodoView create(final @NotBlank String description, final Instant dueAt) {
    final TodoStatus status = dueAt != null && dueAt.isBefore(clock.instant()) ? TodoStatus.PAST_DUE : TodoStatus.NOT_DONE;
    final TodoItem item = new TodoItem(description, clock.instant(), dueAt, status);
    final TodoItem saved = toDoRepository.save(item);
    logger.info("Created a new item: {}", item);
    return toView(saved);
  }

  @Transactional
  public TodoView changeDescription(final long id, final @NotBlank String description) {
    final TodoItem item = findForUpdate(id);
    item.setDescription(description);
    logger.info("Changing description of item {}: {}", item.id(), item);
    return toView(item);
  }

  @Transactional
  public TodoView updateStatus(final long id, final boolean done) {
    final TodoItem item = findForUpdate(id);
    if (done) {
      item.setDone(clock.instant());
    } else {
      item.setNotDone(clock.instant());
    }
    logger.info("Updating status of item {}: {}", item.id(), item);
    return toView(item);
  }

  @Transactional(readOnly = true)
  public TodoViewList getAllItems() {
    return toView(toDoRepository.findAllByOrderByCreatedAtAsc());
  }

  @Transactional(readOnly = true)
  public TodoViewList getNotDoneItems() {
    return toView(toDoRepository.findAllNotDoneItems(clock.instant()));
  }

  private TodoViewList toView(final List<TodoItem> items) {
    return new TodoViewList(items.stream().map(this::toView).toList());
  }

  @Transactional(readOnly = true)
  public TodoView get(final long id) {
    final TodoItem item = find(id);
    return toView(item);
  }

  private TodoView toView(final TodoItem item) {
    return new TodoView(item, item.effectiveStatus(clock.instant()));
  }

  private TodoItem findForUpdate(final long id) {
    final TodoItem item = find(id);
    refreshPastDue(item);
    return item;
  }

  private TodoItem find(final long id) {
    return toDoRepository.findById(id).orElseThrow(() -> new ItemNotFoundException(id));
  }

  private void refreshPastDue(final TodoItem item) {
    if(item.refreshPastDue(clock.instant())) {
      logger.info("Item {} changed to past due: {}", item.id(), item);
    }
  }

}
