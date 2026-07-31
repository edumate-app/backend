package com.example.auth.auth.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class OAuthEmailNotFoundException extends ApplicationException {

  public OAuthEmailNotFoundException() {
    super(
        "OAUTH_EMAIL_NOT_FOUND",
        "Unable to retrieve email from OAuth provider",
        HttpStatus.BAD_REQUEST
    );
  }
}
