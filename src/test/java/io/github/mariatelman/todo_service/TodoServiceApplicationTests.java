package io.github.mariatelman.todo_service;

import io.github.mariatelman.todo_service.api.dto.in.ChangeDescriptionRequest;
import io.github.mariatelman.todo_service.api.dto.in.CreateTodoItemRequest;
import io.github.mariatelman.todo_service.domain.view.TodoView;
import io.github.mariatelman.todo_service.domain.model.TodoStatus;
import io.github.mariatelman.todo_service.domain.view.TodoViewList;
import io.github.mariatelman.todo_service.repo.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TodoServiceApplicationTests {

  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private TodoRepository repository;
  @Autowired
  private Clock clock;

  private Instant futureDueDate;
  private Instant pastDueDate;

  @BeforeEach
  void init() {
    futureDueDate = clock.instant().plus(10, ChronoUnit.DAYS);
    pastDueDate = clock.instant().minus(1, ChronoUnit.DAYS);
    repository.deleteAll();
  }

	@Test
	void shouldCreateAndRetrieveItem() {

    final CreateTodoItemRequest request = new CreateTodoItemRequest("First item description", futureDueDate);
    final ResponseEntity<TodoView> createResponse = restTemplate.postForEntity("/todos", request, TodoView.class);

    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createResponse.getBody()).isNotNull();
    final TodoView createdItem = createResponse.getBody();
    assertThat(createdItem.description()).isEqualTo(request.description());
    assertThat(createdItem.dueAt()).isEqualTo(request.dueAt());
    assertThat(createdItem.status()).isEqualTo(TodoStatus.NOT_DONE.name());

    final long id = createdItem.id();

    final ResponseEntity<TodoView> getResponse = restTemplate.getForEntity("/todos/" + id, TodoView.class);
    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody()).isNotNull();
    final TodoView retrievedItem = getResponse.getBody();
    assertThat(retrievedItem).isEqualTo(createdItem);
	}

  @Test
  void shouldReturn400WhenDescriptionIsEmpty() {
    final CreateTodoItemRequest requestNull = new CreateTodoItemRequest(null, futureDueDate);
    final CreateTodoItemRequest requestBlank = new CreateTodoItemRequest(" ", futureDueDate);

    final ResponseEntity<TodoView> createResponseNull = restTemplate.postForEntity("/todos", requestNull, TodoView.class);
    final ResponseEntity<TodoView> createResponseBlank = restTemplate.postForEntity("/todos", requestBlank, TodoView.class);

    assertEquals(HttpStatus.BAD_REQUEST, createResponseNull.getStatusCode());
    assertEquals(HttpStatus.BAD_REQUEST, createResponseBlank.getStatusCode());
  }

  @Test
  void shouldAllowNullDueDate() {
    final CreateTodoItemRequest request = new CreateTodoItemRequest("First item description", null);
    final ResponseEntity<TodoView> createResponse = restTemplate.postForEntity("/todos", request, TodoView.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createResponse.getBody()).isNotNull();
    final TodoView createdItem = createResponse.getBody();
    assertThat(createdItem.dueAt()).isNull();
  }

  @Test
  void shouldReturn404WhenItemNotFound() {

    final ResponseEntity<TodoView> getResponse = restTemplate.getForEntity("/todos/123456789", TodoView.class);
    assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
  }

  @Test
  void shouldReturn409WhenChangingPastDueItem() {
    final CreateTodoItemRequest request = new CreateTodoItemRequest("First item description", pastDueDate);
    final ResponseEntity<TodoView> createResponse = restTemplate.postForEntity("/todos", request, TodoView.class);
    assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createResponse.getBody()).isNotNull();
    final TodoView createdItem = createResponse.getBody();
    assertThat(createdItem.status()).isEqualTo(TodoStatus.PAST_DUE.name());

    final long id = createdItem.id();
    final ChangeDescriptionRequest changeDescriptionRequest = new ChangeDescriptionRequest("Changed description");
    final ResponseEntity<TodoView> descriptionResponse = restTemplate.exchange(
      "/todos/" + id + "/description",
        HttpMethod.PATCH,
        new HttpEntity<>(changeDescriptionRequest),
        TodoView.class
      );
    assertEquals(HttpStatus.CONFLICT, descriptionResponse.getStatusCode());

    final ResponseEntity<TodoView> markDoneResponse = restTemplate.postForEntity("/todos/" + id + "/done", null, TodoView.class);
    assertEquals(HttpStatus.CONFLICT, markDoneResponse.getStatusCode());
    final ResponseEntity<TodoView> markNotDoneResponse = restTemplate.postForEntity("/todos/" + id + "/not-done", null, TodoView.class);
    assertEquals(HttpStatus.CONFLICT, markNotDoneResponse.getStatusCode());

    final TodoView retrievedItem = restTemplate.getForObject("/todos/" + id, TodoView.class);
    assertThat(retrievedItem.status()).isEqualTo(TodoStatus.PAST_DUE.name());
    assertThat(retrievedItem.description()).isEqualTo(request.description());
  }

  @Test
  void getAllItems() {
    final CreateTodoItemRequest requestPastDue = new CreateTodoItemRequest("First item description", pastDueDate);
    final CreateTodoItemRequest requestFutureDue = new CreateTodoItemRequest("Second item description", futureDueDate);
    final CreateTodoItemRequest requestNotDone = new CreateTodoItemRequest("Third item description", futureDueDate);
    final CreateTodoItemRequest requestNoDue = new CreateTodoItemRequest("Forth item description", null);

    restTemplate.postForObject("/todos", requestPastDue, TodoView.class);
    final TodoView responseFutureDueItem = restTemplate.postForObject("/todos", requestFutureDue, TodoView.class);
    restTemplate.postForObject("/todos/" + responseFutureDueItem.id() + "/done", null, TodoView.class);
    final long notDoneId = restTemplate.postForObject("/todos", requestNotDone, TodoView.class).id();
    final long noDueDateId = restTemplate.postForObject("/todos", requestNoDue, TodoView.class).id();

    final ResponseEntity<TodoViewList> response = restTemplate.getForEntity("/todos", TodoViewList.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    final TodoViewList list = response.getBody();
    assertThat(list.items()).hasSize(2)
      .anyMatch(item -> item.id() == notDoneId)
      .anyMatch(item -> item.id() == noDueDateId);

    final ResponseEntity<TodoViewList> responseAll = restTemplate.getForEntity("/todos?all=true", TodoViewList.class);
    assertThat(responseAll.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(responseAll.getBody()).isNotNull();
    final TodoViewList allItem = responseAll.getBody();
    assertThat(allItem.items()).hasSize(4);
  }

  @Test
  void changeItemAndRetrieve() {
    final CreateTodoItemRequest request = new CreateTodoItemRequest("First item description", futureDueDate);
    final long id = restTemplate.postForObject("/todos", request, TodoView.class).id();

    final ChangeDescriptionRequest changeDescriptionRequest = new ChangeDescriptionRequest("Changed description");
    final ResponseEntity<TodoView> descriptionResponse = restTemplate.exchange(
      "/todos/" + id + "/description",
        HttpMethod.PATCH,
        new HttpEntity<>(changeDescriptionRequest),
        TodoView.class
      );
    assertThat(descriptionResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(descriptionResponse.getBody()).isNotNull();
    assertThat(descriptionResponse.getBody().description()).isEqualTo(changeDescriptionRequest.description());

    final ResponseEntity<TodoView> doneResponse = restTemplate.postForEntity("/todos/" + id + "/done", null, TodoView.class);
    assertThat(doneResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(doneResponse.getBody()).isNotNull();
    assertThat(doneResponse.getBody().status()).isEqualTo(TodoStatus.DONE.name());

    final TodoView retrievedResponse = restTemplate.getForObject("/todos/" + id, TodoView.class);
    assertThat(retrievedResponse.status()).isEqualTo(TodoStatus.DONE.name());
    assertThat(retrievedResponse.doneAt()).isNotNull();

    final ResponseEntity<TodoView> notDoneResponse = restTemplate.postForEntity("/todos/" + id + "/not-done", null, TodoView.class);
    assertThat(notDoneResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(notDoneResponse.getBody()).isNotNull();
    assertThat(notDoneResponse.getBody().status()).isEqualTo(TodoStatus.NOT_DONE.name());
    assertThat(notDoneResponse.getBody().doneAt()).isNull();
    assertThat(notDoneResponse.getBody().description()).isEqualTo(changeDescriptionRequest.description());

    final TodoView retrievedResponse2 = restTemplate.getForObject("/todos/" + id, TodoView.class);
    assertThat(retrievedResponse2.status()).isEqualTo(TodoStatus.NOT_DONE.name());
    assertThat(retrievedResponse2.doneAt()).isNull();


  }

}
