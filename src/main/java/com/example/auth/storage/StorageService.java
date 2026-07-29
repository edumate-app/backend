package com.example.auth.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

  String upload(
      MultipartFile file
  );

  void delete(
      String key
  );
}
