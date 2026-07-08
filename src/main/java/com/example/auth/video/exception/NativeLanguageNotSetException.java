package com.example.auth.video.exception;

public class NativeLanguageNotSetException extends RuntimeException {

  public NativeLanguageNotSetException() {
    super("Native language is not set");
  }
}
