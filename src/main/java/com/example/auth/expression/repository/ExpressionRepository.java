package com.example.auth.expression.repository;

import com.example.auth.expression.entity.Expression;
import com.example.auth.user.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpressionRepository extends JpaRepository<Expression, UUID> {
  List<Expression> findAllByUser(AppUser user);
  Optional<Expression> findByUserAndLangAndLemma(AppUser user, String lang, String lemma);
}
