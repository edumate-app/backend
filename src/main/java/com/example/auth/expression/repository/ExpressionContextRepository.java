package com.example.auth.expression.repository;

import com.example.auth.expression.entity.ExpressionContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpressionContextRepository extends JpaRepository<ExpressionContext, UUID> {
  @Query("""
        SELECT ec FROM ExpressionContext ec
        JOIN FETCH ec.video v
        WHERE ec.expression.id = :expressionId
        ORDER BY ec.savedAt ASC
    """)
  List<ExpressionContext> findAllByExpressionIdWithVideo(@Param("expressionId") UUID expressionId);
}
