package org.tldrtimes.common.domain.article;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.tldrtimes.common.domain.support.SoftDeletableEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
