package com.example.auth.auth.dto;

import com.example.auth.user.entity.AuthProvider;

public record UserDto(String name, String email, String avatarUrl, String nativeLang, AuthProvider provider) {
}
