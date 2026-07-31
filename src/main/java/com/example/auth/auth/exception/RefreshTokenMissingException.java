package com.example.auth.auth.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class RefreshTokenMissingException extends ApplicationException {
  public RefreshTokenMissingException() {
    super(
        "REFRESH_TOKEN_MISSING",
        "Refresh token is missing",
        HttpStatus.UNAUTHORIZED
    );
  }
}
