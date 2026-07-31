package com.example.auth.video.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class NativeLanguageNotSetException extends ApplicationException {

  public NativeLanguageNotSetException() {
    super(
        "NATIVE_LANGUAGE_NOT_SET",
        "Native language is not set",
        HttpStatus.BAD_REQUEST
    );
  }
}
