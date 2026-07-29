package com.example.auth;

import com.example.auth.storage.StorageService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
public class FileController {
  private final StorageService storage;

  public FileController(
      StorageService storage
  ) {
    this.storage = storage;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public String upload(
      @RequestParam MultipartFile file
  ) {

    return storage.upload(file);

  }
}
