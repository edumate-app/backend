package com.example.auth.user.controller;

import com.example.auth.auth.dto.UserDto;
import com.example.auth.user.dto.UpdateNativeLangRequest;
import com.example.auth.user.entity.AppUser;
import com.example.auth.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/me")
  public UserDto me(@AuthenticationPrincipal AppUser user) {
    return userService.me(user);
  }

  @PatchMapping("/native-lang")
  public void updateNativeLang(@AuthenticationPrincipal AppUser user, @RequestBody UpdateNativeLangRequest request) {
    userService.updateNativeLang(user, request.lang());
  }
}
