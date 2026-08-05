package com.example.auth.job.dto;

import com.example.auth.job.record.JobRecord;
import com.example.auth.job.record.JobStatus;
import com.example.auth.job.record.JobType;

import java.util.UUID;

public record JobStatusDto(
    UUID job_id,
    JobType type,
    JobStatus status,
    String step,
    int progress,
    UUID video_uuid,
    String error
) {
  public static JobStatusDto fromVideo(JobRecord job) {
    return new JobStatusDto(
        UUID.fromString(job.id()),
        JobType.VIDEO_IMPORT,
        job.status(),
        job.step().name(),
        job.progress(),
        job.resultId() == null ? null : UUID.fromString(job.resultId()),
        job.error()
    );
  }
}
