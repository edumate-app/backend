package com.example.auth.job.record;

public enum JobStep {
  QUEUED,
  FETCH_VIDEO_INFO,
  FETCH_TRANSCRIPT,
  SAVE_VIDEO,
  ENSURE_LANGUAGE,
  TOKENIZE_SEGMENTS,
  COMPLETED
}