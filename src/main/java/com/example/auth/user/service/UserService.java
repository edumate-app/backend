package com.example.auth.user.service;

import com.example.auth.auth.dto.UserDto;
import com.example.auth.storage.StorageService;
import com.example.auth.user.entity.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final StorageService storageService;

  public UserDto me(AppUser user) {
    String avatarUrl = user.getAvatarKey() != null
        ? storageService.getUrl(user.getAvatarKey())
        : null;
    return new UserDto(
        user.getName(),
        user.getEmail(),
        avatarUrl,
        user.getProvider()
    );
  }
}
