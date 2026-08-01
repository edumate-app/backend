package com.example.auth.expression.controller;

import com.example.auth.expression.dto.*;
import com.example.auth.expression.service.ExpressionService;
import com.example.auth.user.entity.AppUser;
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

  @PostMapping("/analyze")
  public List<WordAnalyzedDto> getAnalysis(@RequestBody AnalyzeRequest request) {
    return expressionService.getAnalysis(request);
  }

  @PostMapping
  public void saveExpressions(@RequestBody SaveExpressionRequest request,
                              @AuthenticationPrincipal AppUser user) {
    expressionService.saveExpressions(request, user);
  }

  @GetMapping
  public List<ExpressionDto> getExpressions (@AuthenticationPrincipal AppUser user) {
    return expressionService.getUserExpressions(user);
  }

  @GetMapping("/{expressionId}/contexts")
  public List<ContextDto> getContexts(@PathVariable UUID expressionId,
                                      @AuthenticationPrincipal AppUser user) {
    return expressionService.getContexts(user, expressionId);
  }
}
