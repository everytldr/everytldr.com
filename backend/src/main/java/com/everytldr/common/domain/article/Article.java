package com.everytldr.common.domain.article;

import com.everytldr.common.domain.license.LicenseInfo;
import com.everytldr.common.domain.support.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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
  private String contentUrl;

  @Column(nullable = false, length = 100)
  private String source;

  @Column(length = 1000)
  private String thumbnailUrl;

  @Column(nullable = false, length = 10)
  private String language;

  @Column(nullable = false)
  private Instant publishedAt;

  @Embedded private LicenseInfo licenseInfo;

  @Column(nullable = false)
  private long viewCount;

  private Article(
      String contentUrl,
      String source,
      String thumbnailUrl,
      String language,
      Instant publishedAt,
      LicenseInfo licenseInfo) {
    this.contentUrl = contentUrl;
    this.source = source;
    this.thumbnailUrl = thumbnailUrl;
    this.language = language;
    this.publishedAt = publishedAt;
    this.licenseInfo = licenseInfo == null ? LicenseInfo.createUnknown() : licenseInfo;
    this.viewCount = 0L;
  }

  public static Article create(
      String contentUrl, String source, String thumbnailUrl, String language, Instant publishedAt) {
    return new Article(
        contentUrl, source, thumbnailUrl, language, publishedAt, LicenseInfo.createUnknown());
  }

  public static Article create(
      String contentUrl,
      String source,
      String thumbnailUrl,
      String language,
      Instant publishedAt,
      LicenseInfo licenseInfo) {
    return new Article(contentUrl, source, thumbnailUrl, language, publishedAt, licenseInfo);
  }

  public void updateThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }
}
