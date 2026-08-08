package com.example.auth.job.record;

import java.util.UUID;

public record JobRecord(
    String id,
    JobType type,
//    String userId,
    JobStatus status,
    JobStep step,
    int progress,
    UUID resultId,
    String error,
    String title,
    long createdAt,      // epoch millis
    long updatedAt
) {
  public static JobRecord create(String id, JobType type) {
    long now = System.currentTimeMillis();
    return new JobRecord(
        id, type,
        JobStatus.PENDING, JobStep.QUEUED, 0,
        null, null, null, now, now
    );
  }
  public JobRecord running(JobStep step, int progress) {
    return new JobRecord(
        id, type, JobStatus.RUNNING, step, progress,
        resultId, null, title, createdAt, System.currentTimeMillis()
    );
  }
  public JobRecord withResult(UUID resultId) {
    return new JobRecord(
        id, type, status, step, progress,
        resultId, error, title, createdAt, System.currentTimeMillis()
    );
  }

  public JobRecord withTitle(String title) {
    return new JobRecord(
        id, type, status, step, progress,
        resultId, error, title, createdAt, System.currentTimeMillis()
    );
  }
  public JobRecord completed(UUID resultId) {
    return new JobRecord(
        id, type, JobStatus.COMPLETED, JobStep.COMPLETED, 100,
        resultId, null, title, createdAt, System.currentTimeMillis()
    );
  }
  public JobRecord failed(String error) {
    return new JobRecord(
        id, type, JobStatus.FAILED, step, progress,
        resultId, error, title, createdAt, System.currentTimeMillis()
    );
  }
}
