package io.github.mariatelman.todo_service.domain.model;

import io.github.mariatelman.todo_service.domain.exceptions.TodoModificationNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToDoItemTest {

  private static final Instant now = Clock.systemUTC().instant();
  private static final Instant created = now.minus(10, ChronoUnit.DAYS);
  private static final Instant dueAt = now.plusSeconds(300);
  private static final Instant pastDueInstant = dueAt.plusSeconds(60);
  private static final Instant beforePastDueInstant = dueAt.minusSeconds(30);

  @Nested
  @DisplayName("Refresh past due tests")
  class RefreshPastDueTests {


    @Test
    void noUpdateIfNoDueDate() {
      final TodoItem item = new TodoItem("Test item", created, null, TodoStatus.NOT_DONE);
      final boolean updated = item.refreshPastDue(pastDueInstant);
      assertFalse(updated);
      assertEquals(TodoStatus.NOT_DONE, item.status());
    }

    @Test
    void noUpdateIfDone() {
      final TodoItem item = new TodoItem("Test item", created, dueAt, TodoStatus.DONE);
      final boolean updated = item.refreshPastDue(pastDueInstant);
      assertFalse(updated);
      assertEquals(TodoStatus.DONE, item.status());
    }

    @Test
    void updatePastDue() {
      final TodoItem item = new TodoItem("Test item", created, dueAt, TodoStatus.NOT_DONE);
      final boolean updated = item.refreshPastDue(pastDueInstant);
      assertTrue(updated);
      assertEquals(TodoStatus.PAST_DUE, item.status());
    }

    @Test
    void noUpdateIfFuture() {
      final TodoItem item = new TodoItem("Test item", created, dueAt, TodoStatus.NOT_DONE);
      final boolean updated = item.refreshPastDue(beforePastDueInstant);
      assertFalse(updated);
      assertEquals(TodoStatus.NOT_DONE, item.status());
    }
  }

  @Nested
  @DisplayName("Set Description tests")
  class SetDescriptionTests {

    private static final String description = "Test description";
    private static final String newDescription = "New description";
    private final TodoItem item = new TodoItem(description, created, null, TodoStatus.NOT_DONE);

    @Test
    void notAllowedToSetDescriptionToNull() {
      assertThrows(IllegalArgumentException.class, () -> item.setDescription(null));
      assertEquals(description, item.description());
    }

    @Test
    void notAllowedToSetDescriptionToBlank() {
      assertThrows(IllegalArgumentException.class, () -> item.setDescription(" "));
      assertEquals(description, item.description());
    }

    @Test
    void notAllowedToUpdatePastDueItem() {
      final TodoItem pastDueItem = createPastDueItem();
      assertThrows(TodoModificationNotAllowedException.class, () -> pastDueItem.setDescription(newDescription));
      assertEquals(description, item.description());
    }

    @Test
    void updatedDescription() {
      item.setDescription(description);
      assertEquals(description, item.description());
    }

  }

  @Nested
  @DisplayName("Set done tests")
  class SetDoneTests {

    @Test
    void errorIfPastDue() {
      final TodoItem pastDueItem = createPastDueItem();
      assertThrows(TodoModificationNotAllowedException.class, () -> pastDueItem.setDone(Instant.now()));
    }

    @Test
    void changedOnce() {
      final TodoItem item = new TodoItem("Test item", created, dueAt, TodoStatus.NOT_DONE);
      item.setDone(now);
      assertEquals(TodoStatus.DONE, item.status());
      assertEquals(now, item.doneAt());
      final Instant nextDoneAt = now.plusSeconds(1);
      item.setDone(nextDoneAt);
      assertEquals(TodoStatus.DONE, item.status());
      assertEquals(now, item.doneAt());
    }

  }

  @Nested
  @DisplayName("Set not-done tests")
  class SetNotDoneTests {


    @Test
    void errorIfPastDue() {
      final TodoItem pastDueItem = createPastDueItem();
      assertThrows(TodoModificationNotAllowedException.class, () -> pastDueItem.setNotDone(pastDueInstant));
    }

    @Test
    void changedToNotDone() {
      final TodoItem item = new TodoItem("Test item", created, dueAt, TodoStatus.DONE);
      item.setNotDone(beforePastDueInstant);
      assertEquals(TodoStatus.NOT_DONE, item.status());
      assertNull(item.doneAt());
    }

    @Test
    void noChangesIfNotDone() {
      final TodoItem item = new TodoItem("Test item", created, dueAt, TodoStatus.NOT_DONE);
      item.setNotDone(beforePastDueInstant);
      assertEquals(TodoStatus.NOT_DONE, item.status());
      assertNull(item.doneAt());
    }

    @Test
    void errorIfPastDueAndDone() {
      final TodoItem pastDueItem = new TodoItem("Test item", created, dueAt, TodoStatus.DONE);
      assertThrows(TodoModificationNotAllowedException.class, () -> pastDueItem.setNotDone(pastDueInstant));
    }

    @Test
    void errorIfPastDueAndNotDone() {
      final TodoItem pastDueItem = new TodoItem("Test item", created, dueAt, TodoStatus.NOT_DONE);
      assertThrows(TodoModificationNotAllowedException.class, () -> pastDueItem.setNotDone(pastDueInstant));
    }

  }

  private TodoItem createPastDueItem() {
    final TodoItem item = new TodoItem("Test item", created, dueAt, TodoStatus.NOT_DONE);
    item.refreshPastDue(pastDueInstant);
    return item;
  }
}
