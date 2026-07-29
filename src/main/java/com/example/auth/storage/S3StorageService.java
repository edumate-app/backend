package com.example.auth.storage;

import com.example.auth.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

  private final S3Client s3;
  private final StorageProperties props;

  @Override
  public String upload(
      MultipartFile file
  ) {

    String key =
        UUID.randomUUID()
            + "_"
            + file.getOriginalFilename();


    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(props.bucket())
            .key(key)
            .contentType(
                file.getContentType()
            )
            .build();


    try {
      s3.putObject(
          request,
          RequestBody.fromInputStream(
              file.getInputStream(),
              file.getSize()
          )
      );

    } catch (Exception e) {

      throw new RuntimeException(e);
    }
    return key;
  }

  @Override
  public void delete(String key) {
    s3.deleteObject(builder ->
        builder
            .bucket(props.bucket())
            .key(key)
            .build()
    );
  }
}
