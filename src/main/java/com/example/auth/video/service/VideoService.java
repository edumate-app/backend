package com.example.auth.video.service;

import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.NlpLanguageDto;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.dto.ImportResponse;
import com.example.auth.video.dto.LanguageDto;
import com.example.auth.video.dto.TranscriptResponseDto;
import com.example.auth.video.entity.Video;
import com.example.auth.video.entity.VideoType;
import com.example.auth.video.repository.VideoRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@AllArgsConstructor
public class VideoService {
  private final NlpClient nlpClient;
  private final VideoRepository videoRepository;

  public List<LanguageDto> getAvailableLang(String url, AppUser user) {
    String videoId = extractVideoId(url);
    if (videoId == null || videoId.length() != 11) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid YouTube URL");
    }

    List<NlpLanguageDto> languages = nlpClient.getAvailableLang(videoId);

    Set<String> importedLangs = new HashSet<>(
        videoRepository.findTargetLangsByVideoIdAndUser(videoId, user)
    );

    return languages.stream()
        .map(lang -> new LanguageDto(
            lang.language(),
            lang.language_code(),
            importedLangs.contains(lang.language_code())
        ))
        .toList();
  }

  public ImportResponse addVideo(String url,String targetLang, AppUser user) {
    String videoId = extractVideoId(url);
    Video saved = videoRepository.save(
        Video.builder()
            .videoId(videoId)
            .thumbnailUrl("https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg")
            .type(detectType(url))
            .targetLang(targetLang)
            .user(user)
            .build()
    );

    return new ImportResponse(saved.getId());
  }

  public TranscriptResponseDto getTranscript(UUID video_uuid) {
    Video video = videoRepository.findById(video_uuid)
        .orElseThrow(() -> new RuntimeException("Video not found"));
    String video_id = video.getVideoId();
    return new TranscriptResponseDto(nlpClient.getTranscript(video_id), video_id);
  }

  private VideoType detectType(String url) {
    if (url.contains("/shorts/")) {
      return VideoType.SHORT;
    }
    return VideoType.VIDEO;
  }

  private String extractVideoId(String url) {
    Pattern pattern = Pattern.compile(
        "(?:youtu\\.be/|youtube\\.com(?:/watch\\?v=|/embed/|/shorts/))([^?&/]+)"
    );

    Matcher matcher = pattern.matcher(url);

    if (matcher.find()) {
      return matcher.group(1);
    }

    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid YouTube URL");
  }
}
