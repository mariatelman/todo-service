package io.github.mariatelman.todo_service.api.controller;

import io.github.mariatelman.todo_service.api.dto.in.ChangeDescriptionRequest;
import io.github.mariatelman.todo_service.api.dto.in.CreateTodoItemRequest;
import io.github.mariatelman.todo_service.domain.view.TodoView;
import io.github.mariatelman.todo_service.application.TodoService;
import io.github.mariatelman.todo_service.domain.view.TodoViewList;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;


@RestController
@Validated
@RequestMapping(path = "/todos")
public class TodoController {

  private final TodoService toDoService;

  public TodoController(final TodoService toDoService) {
    this.toDoService = toDoService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TodoView create(@Valid @NotNull @RequestBody final CreateTodoItemRequest createToDoItemRequest) {
    return toDoService.create(createToDoItemRequest.description(), createToDoItemRequest.dueAt());
  }

  @PatchMapping("/{id}/description")
  public TodoView changeDescription(@PathVariable final long id, @Valid @NotNull @RequestBody final ChangeDescriptionRequest request) {
    return toDoService.changeDescription(id, request.description());
  }

  @PostMapping("/{id}/done")
  public TodoView markAsDone(@PathVariable final long id) {
    return toDoService.updateStatus(id, true);
  }

  @PostMapping("/{id}/not-done")
  public TodoView markAsNotDone(@PathVariable final long id) {
    return toDoService.updateStatus(id, false);
  }

  @GetMapping
  public TodoViewList getAll(@RequestParam(defaultValue = "false") final boolean all) {
    if (all) {
      return toDoService.getAllItems();
    } else {
      return toDoService.getNotDoneItems();
    }
  }

  @GetMapping("/{id}")
  public TodoView get(@PathVariable final long id) {
    return toDoService.get(id);
  }

}
