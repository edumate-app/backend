package com.example.auth.nlp;

import com.example.auth.video.dto.LanguageDto;
import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@AllArgsConstructor
public class NlpClient {

  private final WebClient webClient = WebClient.builder()
      .baseUrl("http://nlp-service:8000")
      .build();

  public List<LanguageDto> getAvailableLang(String videoId) {
    return webClient.get()
        .uri("/lang/{videoId}", videoId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<LanguageDto>>() {})
        .block();
  }
}
