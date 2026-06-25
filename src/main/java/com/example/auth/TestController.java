package com.example.auth;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@AllArgsConstructor
public class TestController {

  private final RestTemplate restTemplate;

  @GetMapping("/test")
  public String helloWorld() {
    return restTemplate.getForObject(
        "http://nlp-service:8000/health",
        String.class
    );
  }
}
