package io.github.mariatelman.todo_service.application;

import io.github.mariatelman.todo_service.domain.exceptions.ItemNotFoundException;
import io.github.mariatelman.todo_service.domain.exceptions.TodoModificationNotAllowedException;
import io.github.mariatelman.todo_service.domain.model.TodoStatus;
import io.github.mariatelman.todo_service.domain.model.TodoItem;
import io.github.mariatelman.todo_service.domain.view.TodoView;
import io.github.mariatelman.todo_service.domain.view.TodoViewList;
import io.github.mariatelman.todo_service.repo.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TodoServiceTest {

  private static final Instant now = Clock.systemUTC().instant();
  private static final Instant created = now.minus(10, java.time.temporal.ChronoUnit.DAYS);
  private static final Instant pastDue = now.minusSeconds(1500);
  private static final Instant futureDue = now.plusSeconds(1500);
  private static final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

  @Mock
  private TodoRepository toDoRepository;

  private TodoService toDoService;

  @BeforeEach
  void init() {
    toDoService = new TodoService(toDoRepository, clock);
  }

  @Nested
  @DisplayName("Create an item")
  class CreateItemTests {

    @Captor
    private ArgumentCaptor<TodoItem> itemCaptor;

    @Test
    void createItem() {
      final TodoItem expectedItem = new TodoItem("Test item", now, futureDue, TodoStatus.NOT_DONE);
      when(toDoRepository.save(any())).thenReturn(expectedItem);
      final TodoView view = toDoService.create("Test item", futureDue);
      verify(toDoRepository).save(itemCaptor.capture());
      final TodoItem actualItem = itemCaptor.getValue();
      assertTodoEquals(expectedItem, actualItem);
      assertFieldsEqual(expectedItem, view);
    }

    @Test
    void createPastDueItem() {
      final TodoItem expectedItem = new TodoItem("Test item", now, pastDue, TodoStatus.PAST_DUE);
      when(toDoRepository.save(any())).thenReturn(expectedItem);
      final TodoView view = toDoService.create("Test item", pastDue);
      verify(toDoRepository).save(itemCaptor.capture());
      final TodoItem actualItem = itemCaptor.getValue();
      assertTodoEquals(expectedItem, actualItem);
      assertFieldsEqual(expectedItem, view);
    }
  }

  @Nested
  @DisplayName("Change descriptions")
  class ChangedDescriptionTests {

    @Test
    void throwsExceptionIfNotFound() {
      when(toDoRepository.findById(1L)).thenReturn(Optional.empty());
      assertThrows(ItemNotFoundException.class, () -> toDoService.changeDescription(1L, "New description"));
    }

    @Test
    void throwsExceptionIfPastDue() {
      when(toDoRepository.findById(1L))
        .thenReturn(Optional.of(new TodoItem("Test item", created, pastDue, TodoStatus.NOT_DONE)));
      assertThrows(TodoModificationNotAllowedException.class, () -> toDoService.changeDescription(1L, "New description"));
    }

    @Test
    void changeDescription() {
      final TodoItem item = new TodoItem("Test item", created, futureDue, TodoStatus.NOT_DONE);
      when(toDoRepository.findById(1L)).thenReturn(Optional.of(item));
      final TodoView view = toDoService.changeDescription(1L, "New description");
      assertEquals("New description", view.description());
    }
  }

  @Nested
  @DisplayName("Update an item status")
  class UpdateStatusTests {

    @Test
    void throwsExceptionIfNotFound() {
      when(toDoRepository.findById(1L)).thenReturn(Optional.empty());
      assertThrows(ItemNotFoundException.class, () -> toDoService.updateStatus(1L, true));
    }

    @Test
    void throwsExceptionIfPastDue() {
      when(toDoRepository.findById(1L))
        .thenReturn(Optional.of(new TodoItem("Test item", created, pastDue, TodoStatus.NOT_DONE)));
      assertThrows(TodoModificationNotAllowedException.class, () -> toDoService.updateStatus(1L, true));
    }

    @Test
    void throwsExceptionIfDoneButPastDue() {
      when(toDoRepository.findById(1L))
        .thenReturn(Optional.of(new TodoItem("Test item", created, pastDue, TodoStatus.DONE)));
      assertThrows(TodoModificationNotAllowedException.class, () -> toDoService.updateStatus(1L, false));
    }

    @Test
    void changingStatusToDone() {
      when(toDoRepository.findById(1L))
        .thenReturn(Optional.of(new TodoItem("Test item", created, futureDue, TodoStatus.NOT_DONE)));
      final TodoView updated = toDoService.updateStatus(1L, true);
      assertEquals(TodoStatus.DONE.name(), updated.status());
      assertEquals(now, updated.doneAt());
    }

    @Test
    void changingStatusToNotDone() {
      when(toDoRepository.findById(1L))
        .thenReturn(Optional.of(new TodoItem("Test item", created, futureDue, TodoStatus.DONE)));
      final TodoView updated = toDoService.updateStatus(1L, false);
      assertEquals(TodoStatus.NOT_DONE.name(), updated.status());
      assertNull(updated.doneAt());
    }
  }

  @Nested
  @DisplayName("Get all items")
  class GetAllItemsTests {

    private static final TodoItem pastDueItem = new TodoItem("Item 1", created, pastDue, TodoStatus.PAST_DUE);
    private static final TodoItem pastDueItem2 = new TodoItem("Item 2", created, pastDue, TodoStatus.PAST_DUE);
    private static final TodoItem doneItem = new TodoItem("Item 3", created, pastDue, TodoStatus.DONE);
    private static final TodoItem doneItem2 = new TodoItem("Item 4", created, futureDue, TodoStatus.DONE);
    private static final TodoItem notDoneItem = new TodoItem("Item 5", created, futureDue, TodoStatus.NOT_DONE);
    private static final TodoItem notDoneItem2 = new TodoItem("Item 6", created, futureDue, TodoStatus.NOT_DONE);

    @Test
    void getAll_noErrorsOnEmptyResult() {
      when(toDoRepository.findAllByOrderByCreatedAtAsc()).thenReturn(List.of());
      final TodoViewList result = toDoService.getAllItems();
      assertEquals(0, result.items().size());
    }

    @Test
    void getNotDone_noErrorsOnEmptyResult() {
      when(toDoRepository.findAllNotDoneItems(any())).thenReturn(List.of());
      final TodoViewList result = toDoService.getNotDoneItems();
      assertEquals(0, result.items().size());
    }

    @Test
    void getAll_allItems() {
      final List<TodoItem> allItems = List.of(pastDueItem, pastDueItem2, doneItem, doneItem2, notDoneItem, notDoneItem2);
      when(toDoRepository.findAllByOrderByCreatedAtAsc()).thenReturn(allItems);
      final TodoViewList result = toDoService.getAllItems();
      final List<TodoView> views = result.items();
      assertEquals(allItems.size(), views.size());
    }

    @Test
    void getNotDone_allItems() {
      final List<TodoItem> notDoneItems = List.of(notDoneItem, notDoneItem2);
      when(toDoRepository.findAllNotDoneItems(any())).thenReturn(notDoneItems);

      final TodoViewList result = toDoService.getNotDoneItems();
      assertEquals(2, result.items().size());
    }
  }

  @Nested
  @DisplayName("Get item by id")
  class GetItemByIdTests {

    @Test
    void throwsExceptionIfNotFound() {
      when(toDoRepository.findById(2L)).thenReturn(Optional.empty());
      assertThrows(ItemNotFoundException.class, () -> toDoService.get(2L));
    }

    @Test
    void getById() {
      final TodoItem item = new TodoItem("Test item", created, futureDue, TodoStatus.NOT_DONE);
      when(toDoRepository.findById(1L)).thenReturn(Optional.of(item));
      final TodoView view = toDoService.get(1L);
      assertFieldsEqual(item, view);
      assertEquals(TodoStatus.NOT_DONE.name(), view.status());
    }
  }

  private void assertTodoEquals(final TodoItem item1, final TodoItem item2) {
    if (item1 == null && item2 == null) {
      return;
    }
    assertNotNull(item1);
    assertNotNull(item2);
    assertEquals(item1, item2);
    assertEquals(item1.description(), item2.description());
    assertEquals(item1.createdAt(), item2.createdAt());
    assertEquals(item1.dueAt(), item2.dueAt());
    assertEquals(item1.doneAt(), item2.doneAt());
    assertEquals(item1.status(), item2.status());
  }

  private void assertFieldsEqual(final TodoItem item, final TodoView view) {
    if (item == null && view == null) {
      return;
    }
    assertNotNull(item);
    assertNotNull(view);
    assertEquals(item.id(), view.id());
    assertEquals(item.description(), view.description());
    assertEquals(item.createdAt(), view.createdAt());
    assertEquals(item.dueAt(), view.dueAt());
    assertEquals(item.doneAt(), view.doneAt());
    assertEquals(item.status().name(), view.status());
  }

}
