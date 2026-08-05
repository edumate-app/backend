package com.example.auth.job;

import com.example.auth.job.record.JobRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.SetSpec;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisJobStore {
  private static final String KEY_PREFIX = "job:";
  private static final Duration DEFAULT_TTL = Duration.ofHours(24);

  private final StringRedisTemplate redis;
  private final JsonMapper jsonMapper;

  public void save(JobRecord job, Duration ttl) {
    write(job, ttl != null ? ttl : DEFAULT_TTL);
  }


  public void update(JobRecord job) {
    try {
      String json = jsonMapper.writeValueAsString(job);
      redis.opsForValue().set(key(job.id()), json, SetSpec::keepTtl);
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize job " + job.id(), e);
    }
  }

  public Optional<JobRecord> find(String jobId) {
    String json = redis.opsForValue().get(key(jobId));
    if (json == null || json.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(jsonMapper.readValue(json, JobRecord.class));
    } catch (JacksonException e) {
      throw new IllegalStateException("Failed to deserialize job " + jobId, e);
    }
  }

  private void write(JobRecord job, Duration ttl) {
    try {
      String json = jsonMapper.writeValueAsString(job);
      redis.opsForValue().set(key(job.id()), json, ttl);
    }catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize job " + job.id(), e);
    }
  }

  private static String key(String jobId) {
    return KEY_PREFIX + jobId;
  }
}
