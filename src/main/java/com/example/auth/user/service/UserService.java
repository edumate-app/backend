package com.example.auth.user.service;

import com.example.auth.auth.dto.UserDto;
import com.example.auth.storage.StorageService;
import com.example.auth.user.entity.AppUser;
import com.example.auth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
  private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5 MB
  private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
      "image/jpeg",
      "image/png",
      "image/webp"
  );

  private final StorageService storageService;
  private final UserRepository userRepository;

  public UserDto me(AppUser user) {
    String avatarUrl = user.getAvatarKey() != null
        ? storageService.getUrl(user.getAvatarKey())
        : null;
    return new UserDto(
        user.getName(),
        user.getEmail(),
        avatarUrl,
        user.getNativeLang(),
        user.getProvider()
    );
  }

  @Transactional
  public UserDto updateAvatar(AppUser user, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is required");
    }

    if (file.getSize() > MAX_AVATAR_SIZE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File must be at most 5 MB");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Allowed formats: JPEG, PNG, WebP"
      );
    }

    UUID oldKey = user.getAvatarKey();
    UUID newKey = storageService.upload(file);

    user.setAvatarKey(newKey);
    userRepository.save(user);

    if (oldKey != null) {
      storageService.delete(oldKey);
    }

    return me(user);
  }

  @Transactional
  public UserDto deleteAvatar(AppUser user) {
    UUID oldKey = user.getAvatarKey();
    user.setAvatarKey(null);
    userRepository.save(user);
    if (oldKey != null) {
      storageService.delete(oldKey);
    }
    return me(user);
  }

  public void updateNativeLang(AppUser user, String lang) {
    user.setNativeLang(lang);
    userRepository.save(user);
  }
}
