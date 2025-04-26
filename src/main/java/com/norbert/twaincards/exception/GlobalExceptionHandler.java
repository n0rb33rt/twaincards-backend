package com.norbert.twaincards.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Глобальний обробник винятків для API
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Обробка винятків типу ResourceNotFoundException
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException exception, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            exception.getMessage(),
            request.getDescription(false),
            LocalDateTime.now()
    );

    log.error("Resource not found exception: {}", exception.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  /**
   * Обробка винятків типу ResourceAlreadyExistsException
   */
  @ExceptionHandler(ResourceAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleResourceAlreadyExistsException(ResourceAlreadyExistsException exception, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            exception.getMessage(),
            request.getDescription(false),
            LocalDateTime.now()
    );

    log.error("Resource already exists exception: {}", exception.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  /**
   * Обробка винятків типу UnauthorizedAccessException
   */
  @ExceptionHandler(UnauthorizedAccessException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorizedAccessException(UnauthorizedAccessException exception, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            exception.getMessage(),
            request.getDescription(false),
            LocalDateTime.now()
    );

    log.error("Unauthorized access exception: {}", exception.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
  }

  /**
   * Обробка винятків типу UserAlreadyExistsException
   */
  @ExceptionHandler(UserAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException exception, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            exception.getMessage(),
            request.getDescription(false),
            LocalDateTime.now()
    );

    log.error("User already exists exception: {}", exception.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
  }

  /**
   * Обробка винятків типу InvalidPasswordException
   */
  @ExceptionHandler(InvalidPasswordException.class)
  public ResponseEntity<ErrorResponse> handleInvalidPasswordException(InvalidPasswordException exception, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            exception.getMessage(),
            request.getDescription(false),
            LocalDateTime.now()
    );

    log.error("Invalid password exception: {}", exception.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Обробка винятків типу ExportException
   */
  @ExceptionHandler(ExportException.class)
  public ResponseEntity<ErrorResponse> handleExportException(ExportException exception, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            exception.getMessage(),
            request.getDescription(false),
            LocalDateTime.now()
    );

    log.error("Export exception: {}", exception.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Обробка винятків валідації
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException exception, WebRequest request) {
    Map<String, String> errors = new HashMap<>();

    exception.getBindingResult().getAllErrors().forEach(error -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    ValidationErrorResponse errorResponse = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            request.getDescription(false),
            LocalDateTime.now(),
            errors
    );

    log.error("Validation exception: {}", errors);
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Обробка всіх інших винятків
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGlobalException(Exception exception, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            exception.getMessage(),
            request.getDescription(false),
            LocalDateTime.now()
    );

    log.error("Global exception: {}", exception.getMessage(), exception);
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /**
   * Клас для зберігання інформації про помилку
   */
  public static class ErrorResponse {
    private final int status;
    private final String message;
    private final String details;
    private final LocalDateTime timestamp;

    public ErrorResponse(int status, String message, String details, LocalDateTime timestamp) {
      this.status = status;
      this.message = message;
      this.details = details;
      this.timestamp = timestamp;
    }

    public int getStatus() {
      return status;
    }

    public String getMessage() {
      return message;
    }

    public String getDetails() {
      return details;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }
  }

  /**
   * Клас для зберігання інформації про помилки валідації
   */
  public static class ValidationErrorResponse extends ErrorResponse {
    private final Map<String, String> errors;

    public ValidationErrorResponse(int status, String message, String details, LocalDateTime timestamp, Map<String, String> errors) {
      super(status, message, details, timestamp);
      this.errors = errors;
    }

    public Map<String, String> getErrors() {
      return errors;
    }
  }
}