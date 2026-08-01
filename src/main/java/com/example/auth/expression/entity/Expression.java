package com.example.auth.expression.entity;

import com.example.auth.expression.dto.PosType;
import com.example.auth.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_expression_user_lang_lemma",
        columnNames = {"user_id", "lang", "lemma"}
    )
)
public class Expression {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  /** Target language of the video this expression was learned from (= LemmaConjugation.lang). */
  @Column(nullable = false, length = 16)
  private String lang;

  @Column(nullable = false)
  private String lemma;

  @Column(nullable = false)
  private String lemmaTranslation;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PosType pos;

  @CreationTimestamp
  @Column(name = "added_at", updatable = false, nullable = false)
  private LocalDateTime addedAt;
}
