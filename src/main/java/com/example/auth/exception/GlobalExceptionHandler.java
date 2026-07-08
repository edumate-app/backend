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
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  @ExceptionHandler(VideoNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleVideoNotFound(
      VideoNotFoundException ex,
      HttpServletRequest request
  ) {

    return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(
            new ErrorResponse(
                Instant.now(),
                404,
                "VIDEO_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
            )
        );
  }

  @ExceptionHandler(NativeLanguageNotSetException.class)
  public ResponseEntity<ErrorResponse> handleNativeLanguage(
      NativeLanguageNotSetException ex,
      HttpServletRequest request
  ) {

    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(
            new ErrorResponse(
                Instant.now(),
                400,
                "NATIVE_LANGUAGE_NOT_SET",
                ex.getMessage(),
                request.getRequestURI()
            )
        );
  }
}