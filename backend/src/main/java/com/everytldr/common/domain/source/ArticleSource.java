package com.everytldr.common.domain.source;

import com.everytldr.common.domain.support.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_article_source_is_active", columnList = "is_active"))
public class ArticleSource extends BaseEntity {
  @Column(nullable = false, length = 100)
  private String name;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false)
  private SourcePolicy policy;

  @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
  private boolean isActive;

  @Column(nullable = false, length = 10)
  private String language;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  private SourceType sourceType;

  private ArticleSource(
      String name, SourcePolicy policy, String language, SourceType sourceType, boolean isActive) {
    this.name = name;
    this.policy = policy;
    this.language = language;
    this.sourceType = sourceType;
    this.isActive = isActive;
  }

  public static ArticleSource create(
      String name, SourcePolicy policy, String language, SourceType sourceType) {
    return new ArticleSource(name, policy, language, sourceType, true);
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }
}
