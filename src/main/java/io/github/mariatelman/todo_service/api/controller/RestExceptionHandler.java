package io.github.mariatelman.todo_service.api.controller;

import io.github.mariatelman.todo_service.domain.exceptions.ItemNotFoundException;
import io.github.mariatelman.todo_service.domain.exceptions.TodoModificationNotAllowedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;


@RestControllerAdvice
public class RestExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ProblemDetail handleDateTimeParseException(final HttpMessageNotReadableException ex) {
    if (ex.getCause() != null && ex.getCause().getCause() instanceof DateTimeParseException) {
      return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid date format. Expected format: yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    }
    logger.warn("Unexpected message parsing exception: {}", ex.getMessage(), ex);
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleIllegalArgumentException(final MethodArgumentNotValidException ex) {
    final FieldError fieldError = ex.getBindingResult().getFieldError();
    final String message = fieldError == null ? "Invalid request" : "Error in parameter '" + fieldError.getField() + "': " + fieldError.getDefaultMessage();
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(ItemNotFoundException.class)
  public ProblemDetail handleItemNotFoundException(final ItemNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(TodoModificationNotAllowedException.class)
  public ProblemDetail handleTodoModificationNotAllowedException(final TodoModificationNotAllowedException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpectedException(final Exception ex) {
    logger.error("Unexpected internal error: {}", ex.getMessage(), ex);
    return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected internal error");
  }


}
