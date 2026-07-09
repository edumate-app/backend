package com.example.auth.auth.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends ApplicationException {

  public InvalidRefreshTokenException() {
    super(
        "INVALID_REFRESH_TOKEN",
        "Refresh token is invalid or expired",
        HttpStatus.UNAUTHORIZED
    );
  }
}