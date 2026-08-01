package com.example.auth.expression.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;
import java.util.UUID;
public class ContextNotFoundException extends ApplicationException {
  public ContextNotFoundException(UUID id) {
    super(
        "CONTEXT_NOT_FOUND",
        "Expression context not found: " + id,
        HttpStatus.NOT_FOUND
    );
  }
}
