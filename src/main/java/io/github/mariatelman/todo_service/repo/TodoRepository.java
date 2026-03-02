package io.github.mariatelman.todo_service.repo;

import io.github.mariatelman.todo_service.domain.model.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface TodoRepository extends JpaRepository<TodoItem, Long> {

  List<TodoItem> findAllByOrderByCreatedAtAsc();

  @Query("SELECT t FROM TodoItem t WHERE t.status = 'NOT_DONE' AND (t.dueAt is NULL or t.dueAt > :now) ORDER BY t.createdAt ASC")
  List<TodoItem> findAllNotDoneItems(@Param("now") Instant now);
}
