package com.example.auth.storage;

import com.example.auth.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

  private final S3Client s3;
  private final StorageProperties props;
  private final S3Presigner presigner;

  @Override
  public UUID upload(
      MultipartFile file
  ) {

    UUID key = UUID.randomUUID();

    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(props.bucket())
            .key(String.valueOf(key))
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
  public void delete(UUID key) {
    s3.deleteObject(builder ->
        builder
            .bucket(props.bucket())
            .key(String.valueOf(key))
            .build()
    );
  }

  @Override
  public String getUrl(UUID key) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(props.bucket())
        .key(String.valueOf(key))
        .build();

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(15))
        .getObjectRequest(getObjectRequest)
        .build();

    return presigner.presignGetObject(presignRequest).url().toString();
  }
}
