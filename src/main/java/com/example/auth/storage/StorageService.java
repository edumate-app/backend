package com.example.auth.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageService {

  UUID upload(
      MultipartFile file
  );

  void delete(
      String key
  );

  String getUrl(UUID key);
}
