package com.example.auth.job;

import com.example.auth.job.dto.JobStatusDto;
import com.example.auth.job.record.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// Server-Sent Events
@Component
public class JobSseService {
  private static final Logger log = LoggerFactory.getLogger(JobSseService.class);
  private static final long TIMEOUT_MS = 5 * 60 * 1000L; // 5 min

  // key = jobId (String)
  private final ConcurrentHashMap<String, CopyOnWriteArrayList<SseEmitter>> emitters =
      new ConcurrentHashMap<>();

  public SseEmitter subscribe(JobStatusDto snapshot) {
    String jobId = snapshot.jobId();
    SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
    // based on computeIfAbsent many peoples can see same job
    emitters.computeIfAbsent(jobId, id -> new CopyOnWriteArrayList<>()).add(emitter);

    emitter.onCompletion(() -> unregister(jobId, emitter));
    emitter.onTimeout(() -> unregister(jobId, emitter));
    emitter.onError(ex -> unregister(jobId, emitter));

    try {
      emitter.send(SseEmitter.event().name("status").data(snapshot));
      JobStatus status = snapshot.status();
      if (JobStatus.COMPLETED == status || JobStatus.FAILED == status) {
        emitter.complete();
        unregister(jobId, emitter);
      }
    } catch (IOException e) {
      log.debug("Failed to send initial SSE snapshot for job {}: {}", jobId, e.getMessage());
      unregister(jobId, emitter);
      emitter.completeWithError(e);
    }

    return emitter;
  }

  public void publish(JobStatusDto dto) {
    String jobId = dto.jobId();
    List<SseEmitter> list = emitters.get(jobId);
    if (list == null || list.isEmpty()) {
      return;
    }

    for (SseEmitter emitter : list) {
      try {
        emitter.send(SseEmitter.event().name("status").data(dto));
      } catch (Exception e) {
        log.debug("SSE publish failed for job {}: {}", jobId, e.getMessage());
        unregister(jobId, emitter);
        try {
          emitter.completeWithError(e);
        } catch (Exception ignored) {
          // emitter already dead
        }
      }
    }
  }

  public void complete(String jobId) {
    List<SseEmitter> list = emitters.remove(jobId);
    if (list == null) {
      return;
    }
    for (SseEmitter emitter : list) {
      try {
        emitter.complete();
      } catch (Exception ignored) {
        // already closed
      }
    }
  }

  private void unregister(String jobId, SseEmitter emitter) {
    List<SseEmitter> list = emitters.get(jobId);
    if (list == null) {
      return;
    }
    list.remove(emitter);
    if (list.isEmpty()) {
      emitters.remove(jobId, list);
    }
  }

  private static boolean isTerminal(String status) {
    return JobStatus.COMPLETED.name().equals(status)
        || JobStatus.FAILED.name().equals(status);
  }
}
