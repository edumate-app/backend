package com.example.auth.auth.controller;

import com.example.auth.auth.dto.AuthTokens;
import com.example.auth.auth.dto.LoginRequest;
import com.example.auth.auth.dto.RegisterRequest;
import com.example.auth.auth.exception.RefreshTokenMissingException;
import com.example.auth.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
    AuthTokens tokens = authService.register(req);

    ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(60)
        .build();


    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(24 * 60 * 60)
        .build();

    return ResponseEntity.ok()
        .headers(headers -> {
          headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
          headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        })
        .build();
  }

  @PostMapping("/login")
  public ResponseEntity<Void> login(@RequestBody LoginRequest req) {
    AuthTokens tokens = authService.login(req);

    ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(60)
        .build();

    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(24 * 60 * 60)
        .build();

    return ResponseEntity.ok()
        .headers(headers -> {
          headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
          headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        })
        .build();
  }

  @PostMapping("/refresh")
  public ResponseEntity<Void> refreshToken(HttpServletRequest request,
                                           HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      System.out.println("Cookies in request:");
      for (Cookie c : cookies) {
        System.out.println("Cookie: " + c.getName() + " = " + c.getValue());
      }
    } else {
      System.out.println("No cookies in request");
    }

    String refreshToken = Arrays.stream(Optional.ofNullable(cookies).orElse(new Cookie[0]))
        .filter(c -> c.getName().equals("refresh_token"))
        .findFirst()
        .map(Cookie::getValue)
        .orElseThrow(RefreshTokenMissingException::new);

    System.out.println("Refresh token found: " + refreshToken);

    String userAgent = request.getHeader("User-Agent");
    String ipAddress = request.getRemoteAddr();
    System.out.println("User-Agent: " + userAgent + ", IP: " + ipAddress);

    AuthTokens tokens = authService.refresh(refreshToken);

    ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.accessToken())
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(60)
        .build();

    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", tokens.refreshToken())
        .httpOnly(true)
        .secure(false)
        .path("/")
        .maxAge(24 * 60 * 60)
        .sameSite("Lax")
        .build();

    System.out.println("Refresh token cookie set in response");

    return ResponseEntity.ok()
        .headers(headers -> {
          headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
          headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        })
        .build();
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {

    ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(0)
        .build();

    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(0)
        .build();

    return ResponseEntity.ok()
        .headers(headers -> {
          headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
          headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        })
        .build();
  }
}
