package com.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Configuration
public class S3Config {
  @Bean
  public S3Client s3Client(
      StorageProperties props
  ) {

    return S3Client.builder()
        .endpointOverride(
            URI.create(props.endpoint())
        )
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                    props.accessKey(),
                    props.secretKey()
                )
            )
        )
        .region(
            Region.of(props.region())
        )
        .forcePathStyle(true)
        .build();
  }
}