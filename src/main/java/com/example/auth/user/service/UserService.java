package com.example.auth.user.service;

import com.example.auth.auth.dto.UserDto;
import com.example.auth.user.entity.AppUser;
import org.springframework.stereotype.Service;

@Service
public class UserService {
  public UserDto me(AppUser user) {
    return new UserDto(
        user.getName(),
        user.getEmail(),
        user.getAvatarUrl(),
        user.getProvider()
    );
  }
}
