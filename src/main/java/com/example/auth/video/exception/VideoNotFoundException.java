package com.example.auth.video.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class VideoNotFoundException extends ApplicationException {
  public VideoNotFoundException(UUID id) {
    super(
        "VIDEO_NOT_FOUND",
        "Video not found: " + id,
        HttpStatus.NOT_FOUND
    );
  }
}
