package com.example.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ApplicationException extends RuntimeException {

  private final String code;
  private final HttpStatus status;


  protected ApplicationException(
      String code,
      String message,
      HttpStatus status
  ) {
    super(message);
    this.code = code;
    this.status = status;
  }
}
