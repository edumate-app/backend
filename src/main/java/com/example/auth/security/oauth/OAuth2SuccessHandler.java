package com.example.auth.security.oauth;

import com.example.auth.auth.service.RefreshTokenService;
import com.example.auth.security.jwt.JwtService;
import com.example.auth.storage.AvatarStorageService;
import com.example.auth.storage.StorageService;
import com.example.auth.user.entity.AppUser;
import com.example.auth.user.entity.AuthProvider;
import com.example.auth.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final StorageService storageService;
  private final AvatarStorageService avatarStorageService;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request,
      HttpServletResponse response,
      Authentication authentication) throws IOException {

    OAuthPrincipal principal = (OAuthPrincipal) authentication.getPrincipal();

    String provider = ((OAuth2AuthenticationToken) authentication)
        .getAuthorizedClientRegistrationId()
        .toUpperCase(); // "GOOGLE" or "GITHUB"

    System.out.println(
        "Email: %s | Name: %s | Avatar: %s | Provider: %s"
            .formatted(
                principal.getEmail(),
                principal.getName(),
                principal.getAvatarUrl(),
                provider
            )
    );

    AppUser user = userRepository.findByEmail(principal.getEmail())
        .orElseGet(() -> {
          UUID avatarKey = avatarStorageService
              .fetchAndStore(principal.getAvatarUrl(), principal.getEmail())
              .orElse(null);

          return userRepository.save(
              AppUser.builder()
                  .email(principal.getEmail())
                  .name(principal.getName())
                  .avatarKey(avatarKey)
                  .provider(AuthProvider.valueOf(provider))
                  .build()
          );
        });

    String accessToken = jwtService.generateToken(user.getEmail(), user.getId());
    String refreshToken = refreshTokenService.createRefreshToken(user);

    ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(15 * 60) // 15 minutes
        .build();

    ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true)
        .secure(false)
        .path("/")
        .sameSite("Lax")
        .maxAge(24 * 60 * 60) // 24 hours
        .build();

    response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
    response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    response.sendRedirect("http://localhost:5173/dashboard");
  }
}