package org.tldrtimes.common.domain.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.tldrtimes.common.domain.support.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints = @UniqueConstraint(name = "uk_rss_source_url", columnNames = "url"),
    indexes = @Index(name = "idx_rss_source_is_active", columnList = "is_active"))
public class RssSource extends BaseEntity {
  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 500)
  private String url;

  @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
  private boolean isActive;

  @Column(nullable = false, length = 10)
  private String language;

  private RssSource(String name, String url, String language, boolean isActive) {
    this.name = name;
    this.url = url;
    this.language = language;
    this.isActive = isActive;
  }

  public static RssSource create(String name, String url, String language) {
    return new RssSource(name, url, language, true);
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }
}
