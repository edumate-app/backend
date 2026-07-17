package com.example.auth.nlp;

import com.example.auth.expression.dto.WordAnalyzedDto;
import com.example.auth.nlp.dto.AnalyzeRequest;
import com.example.auth.nlp.dto.NlpLanguageDto;
import com.example.auth.nlp.dto.NlpTranscriptRequest;
import com.example.auth.nlp.dto.VideoInfo;
import com.example.auth.video.dto.TranscriptSegmentDto;
import io.netty.channel.ChannelOption;
import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;

@Service
@AllArgsConstructor
public class NlpClient {

  private final WebClient webClient = WebClient.builder()
      .baseUrl("http://nlp-service:8000")
      .clientConnector(new ReactorClientHttpConnector(
          HttpClient.create()
              .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
              .responseTimeout(Duration.ofSeconds(15))
      ))
      .build();

  public List<WordAnalyzedDto> getAnalysis(AnalyzeRequest request) {
    return webClient.post()
        .uri("/analyze")
        .bodyValue(request)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<List<WordAnalyzedDto>>() {})
        .timeout(Duration.ofSeconds(30))
        .block();
  }


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

  public VideoInfo getVideoInfo(String videoId) {

    return webClient.get()
        .uri("/info/{videoId}", videoId)
        .retrieve()
        .bodyToMono(new ParameterizedTypeReference<VideoInfo>() {})
        .block();
  }
}
