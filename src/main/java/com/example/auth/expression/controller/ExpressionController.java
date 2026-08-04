package com.example.auth.expression.controller;

import com.example.auth.expression.dto.*;
import com.example.auth.expression.service.ExpressionService;
import com.example.auth.user.entity.AppUser;
import com.example.auth.video.service.TranscriptAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/expression")
@RequiredArgsConstructor
public class ExpressionController {
  private final ExpressionService expressionService;
  private final TranscriptAnalysisService transcriptAnalysisService;

  @PostMapping("/analyze")
  public List<WordAnalyzedDto> getAnalysis(@RequestBody AnalyzeRequest request) {
    return transcriptAnalysisService.getAnalysis(request);
  }

  @PostMapping
  public void saveExpressions(@RequestBody SaveExpressionRequest request,
                              @AuthenticationPrincipal AppUser user) {
    expressionService.saveExpressions(request, user);
  }

  @GetMapping
  public ExpressionListResponse getExpressions (@AuthenticationPrincipal AppUser user) {
    return expressionService.getUserExpressions(user);
  }

  @GetMapping("/{expressionId}/contexts")
  public List<ContextDto> getContexts(@PathVariable UUID expressionId,
                                      @AuthenticationPrincipal AppUser user) {
    return expressionService.getContexts(user, expressionId);
  }

  @DeleteMapping("/{expressionId}")
  public void deleteExpression(@PathVariable UUID expressionId,
                               @AuthenticationPrincipal AppUser user) {
    expressionService.deleteExpression(user, expressionId);
  }

  @DeleteMapping("/{expressionId}/context/{contextId}")
  public void deleteContext(@PathVariable UUID expressionId,
                            @PathVariable UUID contextId,
                            @AuthenticationPrincipal AppUser user) {
    expressionService.deleteContext(user, expressionId, contextId);
  }
}
