package com.example.auth.expression.repository;

import com.example.auth.expression.entity.ExpressionContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExpressionContextRepository extends JpaRepository<ExpressionContext, UUID> {}
