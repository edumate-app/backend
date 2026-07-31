package com.example.auth.security.oauth;

import com.example.auth.auth.exception.OAuthEmailNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final RestTemplate restTemplate;

  @Override
  public OAuthPrincipal loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(request);

    String email = oAuth2User.getAttribute("email");
    if (email == null) {
      email = fetchGithubEmail(request.getAccessToken().getTokenValue());
    }

    String name = oAuth2User.getAttribute("login");
    String avatarUrl = oAuth2User.getAttribute("avatar_url");

    return new OAuthPrincipal(email, name, avatarUrl, oAuth2User.getAttributes());
  }

  private String fetchGithubEmail(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(token);
    headers.set("User-Agent", "Spring-OAuth2-App");
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));

    try {
      ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
          "https://api.github.com/user/emails",
          HttpMethod.GET,
          new HttpEntity<>(null, headers),
          new ParameterizedTypeReference<>() {
          }
      );

      List<Map<String, Object>> emails = response.getBody();
      if (emails != null && !emails.isEmpty()) {
        return emails.stream()
            .filter(e -> Boolean.TRUE.equals(e.get("primary")))
            .map(e -> (String) e.get("email"))
            .findFirst()
            .orElse((String) emails.getFirst().get("email"));
      }
    } catch (Exception e) {
      System.out.println("Błąd pobierania emaila z GitHub: " + e.getMessage());
    }

    throw new OAuthEmailNotFoundException();
  }
}