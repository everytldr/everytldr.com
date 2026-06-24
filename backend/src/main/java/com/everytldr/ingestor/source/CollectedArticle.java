package com.everytldr.ingestor.source;

import com.everytldr.common.domain.license.LicenseInfo;
import java.time.Instant;

public record CollectedArticle(
    String contentUrl,
    String sourceName,
    String thumbnailUrl,
    String language,
    Instant publishedAt,
    LicenseInfo licenseInfo) {
  public CollectedArticle(
      String contentUrl,
      String sourceName,
      String thumbnailUrl,
      String language,
      Instant publishedAt) {
    this(contentUrl, sourceName, thumbnailUrl, language, publishedAt, LicenseInfo.createUnknown());
  }
}
