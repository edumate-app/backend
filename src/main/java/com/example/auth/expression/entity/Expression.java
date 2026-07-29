package com.example.auth.expression.entity;

import com.example.auth.expression.dto.NumberType;
import com.example.auth.expression.dto.PosType;
import com.example.auth.expression.dto.VerbConjugationForm;
import com.example.auth.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Expression {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false)
  private String lemma;

  @Column(nullable = false)
  private String translation;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private PosType pos;

  @ElementCollection
  @CollectionTable(
      name = "expression_conjugation_forms",
      joinColumns = @JoinColumn(name = "expression_id")
  )
  @OrderColumn(name = "sort_order")
  @Builder.Default
  private List<VerbConjugationForm> conjugation = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "added_at", updatable = false, nullable = false)
  private LocalDateTime addedAt;
}
