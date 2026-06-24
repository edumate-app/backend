package com.example.auth.security.oauth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class OAuthPrincipal implements OidcUser {
  private final String email;
  private final String name;
  private final String avatarUrl;
  private final Map<String, Object> attributes;

  public OAuthPrincipal(String email, String name, String avatarUrl, Map<String, Object> attributes) {
    this.email = email;
    this.name = name;
    this.avatarUrl = avatarUrl;
    this.attributes = Collections.unmodifiableMap(attributes);
  }

  // OidcUser / OAuth2User
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getName() {
    return name;
  }

  // OidcUser (Google)
  @Override
  public Map<String, Object> getClaims() {
    return attributes;
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return null;
  }

  @Override
  public OidcIdToken getIdToken() {
    return null;
  }
}
