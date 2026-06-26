package com.example.auth.video.controller;

import com.example.auth.nlp.NlpClient;
import com.example.auth.video.dto.LanguageDto;
import com.example.auth.video.dto.ValidationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {
  private final NlpClient nlpClient;
  @PostMapping("/validation")
  public List<LanguageDto> validation(@RequestParam String url) {
    System.out.println(url);
    // 1. extract video_id from url
    String video_id = extractVideoId(url);
    System.out.println(video_id);
    // 2. check if video exist
    if (video_id == null || video_id.length() != 11) {
      throw new IllegalArgumentException("Invalid video id");
    }
    // 3.Check available languages for this video
    return nlpClient.getAvailableLang(video_id);
  }

  private String extractVideoId(String url) {
    Pattern pattern = Pattern.compile(
        "(?:youtu\\.be/|youtube\\.com(?:/watch\\?v=|/embed/|/shorts/))([^?&/]+)"
    );

    Matcher matcher = pattern.matcher(url);

    if (matcher.find()) {
      return matcher.group(1);
    }

    throw new IllegalArgumentException("Invalid YouTube URL");
  }
}
