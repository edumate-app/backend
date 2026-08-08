package com.example.auth.job.dto;

import com.example.auth.job.record.JobRecord;
import com.example.auth.job.record.JobStatus;
import com.example.auth.job.record.JobType;

import java.util.UUID;

public record JobStatusDto(
    String jobId,
    JobType type,
    JobStatus status,
    String step,
    int progress,
    UUID video_uuid,
    String error,
    String title
) {
  public static JobStatusDto fromVideo(JobRecord job) {
    return new JobStatusDto(
        job.id(),
        JobType.VIDEO_IMPORT,
        job.status(),
        job.step().name(),
        job.progress(),
        job.resultId() == null ? null : job.resultId(),
        job.error(),
        job.title()
    );
  }
}
