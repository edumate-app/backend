package com.example.auth.expression.service;

import com.example.auth.expression.dto.WordAnalyzedDto;
import com.example.auth.nlp.NlpClient;
import com.example.auth.nlp.dto.AnalyzeRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@AllArgsConstructor
public class ExpressionService {
  private final NlpClient nlpClient;

  public List<WordAnalyzedDto> getAnalysis(AnalyzeRequest request) {
    return nlpClient.getAnalysis(request);
  }
}
