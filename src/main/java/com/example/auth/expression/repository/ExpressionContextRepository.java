package com.example.auth.expression.repository;

import com.example.auth.expression.entity.Expression;
import com.example.auth.expression.entity.ExpressionContext;
import com.example.auth.video.entity.TranscriptSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpressionContextRepository extends JpaRepository<ExpressionContext, UUID> {
  @Query("""
        SELECT ec FROM ExpressionContext ec
        JOIN FETCH ec.video v
        WHERE ec.expression.id = :expressionId
        ORDER BY ec.savedAt ASC
    """)
  List<ExpressionContext> findAllByExpressionIdWithVideo(@Param("expressionId") UUID expressionId);

  @Query("""
    SELECT ec.expression.id, COUNT(ec)
    FROM ExpressionContext ec
    WHERE ec.expression.id IN :expressionIds
    GROUP BY ec.expression.id
    """)
  List<Object[]> countByExpressionIds(@Param("expressionIds") Collection<UUID> expressionIds);
  Optional<ExpressionContext> findByExpressionAndTranscriptSegment(
      Expression expression,
      TranscriptSegment transcriptSegment
  );
}
