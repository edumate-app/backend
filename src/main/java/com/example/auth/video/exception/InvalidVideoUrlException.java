package com.example.auth.video.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class InvalidVideoUrlException extends ApplicationException {

  public InvalidVideoUrlException() {
    super(
        "INVALID_VIDEO_URL",
        "Invalid YouTube URL",
        HttpStatus.BAD_REQUEST
    );
  }
}
