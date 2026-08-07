package com.example.auth.job;

import com.example.auth.exception.ApplicationException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public class JobNotFoundException extends ApplicationException {
  public JobNotFoundException(UUID jobId) {
    super(
        "JOB_NOT_FOUND",
        "Job not found: " + jobId,
        HttpStatus.NOT_FOUND
    );
  }
}
