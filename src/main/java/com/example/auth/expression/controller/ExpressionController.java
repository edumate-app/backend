package com.example.auth.expression.controller;

import com.example.auth.expression.dto.WordAnalyzedDto;
import com.example.auth.expression.service.ExpressionService;
import com.example.auth.nlp.dto.AnalyzeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/expression")
@RequiredArgsConstructor
public class ExpressionController {

  private final ExpressionService expressionService;

  @PostMapping("/analyze")
  public List<WordAnalyzedDto> getAnalysis(@RequestBody AnalyzeRequest request) {
    return expressionService.getAnalysis(request);
  }
}
