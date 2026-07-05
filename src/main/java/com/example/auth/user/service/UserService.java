package com.example.auth.user.service;

import com.example.auth.auth.dto.UserDto;
import com.example.auth.user.entity.AppUser;
import com.example.auth.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  public UserDto me(AppUser user) {
    return new UserDto(
        user.getName(),
        user.getEmail(),
        user.getAvatarUrl(),
        user.getNativeLang(),
        user.getProvider()
    );
  }

  public void updateNativeLang(AppUser user, String lang) {
    user.setNativeLang(lang);
    userRepository.save(user);
  }
}
