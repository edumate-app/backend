package com.example.auth.nlp;

import com.example.auth.nlp.dto.NlpLanguageDto;
import com.example.auth.nlp.dto.NlpTranscriptRequest;
import com.example.auth.video.dto.TranscriptSegmentDto;
import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class NlpClient {

  private final WebClient webClient = WebClient.builder()
      .baseUrl("http://nlp-service:8000")
      .build();

  public List<NlpLanguageDto> getAvailableLang(String videoId) {
    return webClient.get()
        .uri("/lang/{videoId}", videoId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<NlpLanguageDto>>() {})
        .block();
  }

  public List<TranscriptSegmentDto> getTranscript(String videoId, NlpTranscriptRequest request) {

    return webClient.post()
        .uri("/transcript/{videoId}", videoId)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<TranscriptSegmentDto>>() {})
        .block();
  }
}
