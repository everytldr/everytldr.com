package org.tldrtimes.common.domain.article;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_article_like_article_ip",
            columnNames = {"article_id", "ip_hash"}),
    indexes =
        @Index(name = "idx_article_like_article_active", columnList = "article_id, is_active"))
public class ArticleLike extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "article_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_article_like_article"))
  private Article article;

  @Column(columnDefinition = "CHAR(64) NOT NULL")
  private String ipHash;

  @Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
  private boolean isActive;

  private ArticleLike(Article article, String ipHash, boolean isActive) {
    this.article = article;
    this.ipHash = ipHash;
    this.isActive = isActive;
  }

  public static ArticleLike create(Article article, String ipHash) {
    return new ArticleLike(article, ipHash, true);
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }
}
