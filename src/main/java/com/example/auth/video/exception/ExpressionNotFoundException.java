package com.example.auth.video.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class ExpressionNotFoundException extends ApplicationException {
  public ExpressionNotFoundException(UUID id) {
    super(
        "EXPRESSION_NOT_FOUND",
        "Expression not found: " + id,
        HttpStatus.NOT_FOUND
    );
  }
}