package com.example.auth.storage;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Getter
@RequiredArgsConstructor
public class ByteArrayMultipartFile implements MultipartFile {

  private final String name;
  private final String originalFilename;
  private final String contentType;
  private final byte[] content;

  @Override
  public boolean isEmpty() {
    return content.length == 0;
  }

  @Override
  public long getSize() {
    return content.length;
  }

  @Override
  @NonNull
  public byte[] getBytes() {
    return content;
  }

  @Override
  @NonNull
  public InputStream getInputStream() {
    return new ByteArrayInputStream(content);
  }

  @Override
  public void transferTo(@NonNull java.io.File dest) throws IOException, IllegalStateException {
    java.nio.file.Files.write(dest.toPath(), content);
  }
}
