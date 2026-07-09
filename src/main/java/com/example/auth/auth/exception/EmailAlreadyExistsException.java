package com.example.auth.auth.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends ApplicationException {

  public EmailAlreadyExistsException(String email) {
    super(
        "EMAIL_ALREADY_EXISTS",
        "Email already in use",
        HttpStatus.CONFLICT
    );
  }
}
