package com.example.auth.job.record;

import java.util.UUID;

public record JobRecord(
    UUID id,
    JobType type,
    String userId,
    JobStatus status,
    JobStep step,
    int progress,
    UUID resultId,
    String error,
    long createdAt,      // epoch millis
    long updatedAt
) {
  public static JobRecord create(UUID id, JobType type, String userId) {
    long now = System.currentTimeMillis();
    return new JobRecord(
        id, type, userId,
        JobStatus.PENDING, JobStep.QUEUED, 0,
        null, null, now, now
    );
  }
  public JobRecord running(JobStep step, int progress) {
    return new JobRecord(
        id, type, userId, JobStatus.RUNNING, step, progress,
        resultId, null, createdAt, System.currentTimeMillis()
    );
  }
  public JobRecord withResult(UUID resultId) {
    return new JobRecord(
        id, type, userId, status, step, progress,
        resultId, error, createdAt, System.currentTimeMillis()
    );
  }
  public JobRecord completed(UUID resultId) {
    return new JobRecord(
        id, type, userId, JobStatus.COMPLETED, JobStep.COMPLETED, 100,
        resultId, null, createdAt, System.currentTimeMillis()
    );
  }
  public JobRecord failed(String error) {
    return new JobRecord(
        id, type, userId, JobStatus.FAILED, step, progress,
        resultId, error, createdAt, System.currentTimeMillis()
    );
  }
}
