package com.everytldr.common.domain.article;

import com.everytldr.common.domain.support.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(indexes = @Index(name = "idx_article_published_at", columnList = "published_at"))
public class Article extends SoftDeletableEntity {
  @Column(nullable = false, length = 1000)
  private String sourceUrl;

  @Column(nullable = false, length = 100)
  private String source;

  @Column(length = 1000)
  private String thumbnailUrl;

  @Column(nullable = false, length = 10)
  private String language;

  @Column(nullable = false)
  private Instant publishedAt;

  private Article(
      String sourceUrl, String source, String thumbnailUrl, String language, Instant publishedAt) {
    this.sourceUrl = sourceUrl;
    this.source = source;
    this.thumbnailUrl = thumbnailUrl;
    this.language = language;
    this.publishedAt = publishedAt;
  }

  public static Article create(
      String sourceUrl, String source, String thumbnailUrl, String language, Instant publishedAt) {
    return new Article(sourceUrl, source, thumbnailUrl, language, publishedAt);
  }
}
