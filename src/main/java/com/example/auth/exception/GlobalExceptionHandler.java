package com.example.auth.exception;

import com.example.auth.video.exception.NativeLanguageNotSetException;
import com.example.auth.video.exception.VideoNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {


  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ErrorResponse> handleApplicationException(
      ApplicationException ex,
      HttpServletRequest request
  ) {

    return ResponseEntity
        .status(ex.getStatus())
        .body(
            new ErrorResponse(
                Instant.now(),
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage(),
                request.getRequestURI()
            )
        );
  }
}