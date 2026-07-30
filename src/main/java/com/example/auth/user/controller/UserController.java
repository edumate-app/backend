package com.example.auth.user.controller;

import com.example.auth.auth.dto.UserDto;
import com.example.auth.user.entity.AppUser;
import com.example.auth.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public UserDto me(@AuthenticationPrincipal AppUser user) {
    return userService.me(user);
  }

  @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public UserDto updateAvatar(
      @AuthenticationPrincipal AppUser user,
      @RequestParam("file") MultipartFile file
  ) {
    return userService.updateAvatar(user, file);
  }

  @DeleteMapping("/avatar")
  public UserDto deleteAvatar(
      @AuthenticationPrincipal AppUser user
  ) {
    return userService.deleteAvatar(user);
  }
}
