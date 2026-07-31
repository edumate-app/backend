package com.example.auth.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarStorageService {

  private final StorageService storageService;

  private final HttpClient httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  public Optional<UUID> fetchAndStore(String providerAvatarUrl, String userIdentifier) {
    if (providerAvatarUrl == null || providerAvatarUrl.isBlank()) {
      return Optional.empty();
    }

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(providerAvatarUrl))
          .timeout(Duration.ofSeconds(5))
          .GET()
          .build();

      HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

      if (response.statusCode() != 200) {
        return Optional.empty();
      }

      String contentType = response.headers()
          .firstValue("Content-Type")
          .orElse("image/jpeg");

      String extension = switch (contentType) {
        case "image/png" -> "png";
        case "image/webp" -> "webp";
        default -> "jpg";
      };

      byte[] bytes = response.body();
      String filename = "avatar-%s.%s".formatted(sanitize(userIdentifier), extension);

      ByteArrayMultipartFile multipartFile =
          new ByteArrayMultipartFile("avatar", filename, contentType, bytes);

      UUID storedKey = storageService.upload(multipartFile);
      return Optional.of(storedKey);

    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private String sanitize(String input) {
    return input.replaceAll("[^a-zA-Z0-9]", "_");
  }
}
