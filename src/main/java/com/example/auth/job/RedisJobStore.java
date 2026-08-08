package com.example.auth.job;

import com.example.auth.job.record.JobRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.SetSpec;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
public class RedisJobStore {
  private static final String KEY_PREFIX = "job:";
  private static final String USERS_SUFFIX = ":users";
  private static final String USER_PREFIX = "user:";
  private static final String JOBS_SUFFIX = ":jobs";
  private static final Duration DEFAULT_TTL = Duration.ofHours(24);

  private final StringRedisTemplate redis;
  private final JsonMapper jsonMapper;

  public void save(JobRecord job, UUID userId) {
    try {
      String json = jsonMapper.writeValueAsString(job);

      redis.opsForValue().set(
          key(job.id()),
          json,
          DEFAULT_TTL
      );

      addUser(job.id(), userId);

      redis.expire(
          usersKey(job.id()),
          DEFAULT_TTL
      );

    }catch(JacksonException e) {
      throw new IllegalStateException(
          "Failed to serialize job " + job.id(),
          e
      );
    }
  }

  public void addUser(String jobId, UUID userId) {
    redis.opsForSet().add(usersKey(jobId), userId.toString());
    redis.opsForSet().add(userJobsKey(userId), jobId);
    redis.expire(userJobsKey(userId), DEFAULT_TTL);
  }

  public Set<UUID> getUserIds(String jobId) {
    Set<String> members = redis.opsForSet().members(usersKey(jobId));
    if (members == null || members.isEmpty()) {
      return Set.of();
    }
    Set<UUID> result = new HashSet<>();
    for (String member : members) {
      try {
        result.add(UUID.fromString(member));
      } catch (IllegalArgumentException ignored) {
        // skip malformed entries
      }
    }
    return result;
  }

  public boolean hasAccess(String jobId, UUID userId) {
    return Boolean.TRUE.equals(
        redis.opsForSet().isMember(
            usersKey(jobId),
            userId.toString()
        )
    );
  }

  public List<JobRecord> findAllByUser(UUID userId) {
    Set<String> jobIds = redis.opsForSet().members(userJobsKey(userId));
    if (jobIds == null || jobIds.isEmpty()) {
      return List.of();
    }

    List<String> keys = jobIds.stream().map(id -> KEY_PREFIX + id).toList();
    List<String> jsons = redis.opsForValue().multiGet(keys);
    if (jsons == null) {
      return List.of();
    }

    List<JobRecord> result = new ArrayList<>();
    Iterator<String> idIt = jobIds.iterator();
    for (String json : jsons) {
      String jobId = idIt.next();
      if (json == null || json.isBlank()) {
        redis.opsForSet().remove(userJobsKey(userId), jobId);
        continue;
      }
      try {
        result.add(jsonMapper.readValue(json, JobRecord.class));
      } catch (JacksonException e) {
        throw new IllegalStateException("Failed to deserialize job " + jobId, e);
      }
    }
    return result;
  }

  public Optional<JobRecord> find(String jobId, UUID userId) {

    if (!hasAccess(jobId, userId)) {
      return Optional.empty();
    }

    String json = redis.opsForValue().get(key(jobId));

    if (json == null || json.isBlank()) {
      return Optional.empty();
    }

    try {
      return Optional.of(
          jsonMapper.readValue(json, JobRecord.class)
      );
    } catch (JacksonException e) {
      throw new IllegalStateException(
          "Failed to deserialize job " + jobId,
          e
      );
    }
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

  private void write(JobRecord job) {
    try {
      String json = jsonMapper.writeValueAsString(job);
      redis.opsForValue().set(key(job.id()), json, DEFAULT_TTL);
    }catch (JacksonException e) {
      throw new IllegalStateException("Failed to serialize job " + job.id(), e);
    }
  }

  private static String key(String jobId) {
    return KEY_PREFIX + jobId;
  }

  private static String userJobsKey(UUID userId) {
    return USER_PREFIX + userId + JOBS_SUFFIX;
  }
  private static String usersKey(String jobId) {
    return KEY_PREFIX + jobId + USERS_SUFFIX;
  }
}
