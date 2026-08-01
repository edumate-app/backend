package com.example.auth.video.exception;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class TranscriptSegmentNotFoundException extends ApplicationException {
  public TranscriptSegmentNotFoundException(UUID videoId, int contextIndex) {
    super(
        "TRANSCRIPT_SEGMENT_NOT_FOUND",
        "Transcript segment not found at index " + contextIndex + " for video " + videoId,
        HttpStatus.NOT_FOUND
    );
  }
}
