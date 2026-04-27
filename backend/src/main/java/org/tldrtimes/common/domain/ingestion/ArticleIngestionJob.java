package org.tldrtimes.common.domain.ingestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.tldrtimes.common.domain.article.Article;
import org.tldrtimes.common.domain.support.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_article_ingestion_job_article",
          columnNames = "article_id"),
      @UniqueConstraint(
          name = "uk_article_ingestion_job_url_hash",
          columnNames = "url_hash")
    })
public class ArticleIngestionJob extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "article_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_article_ingestion_job_article"))
  private Article article;

  @Column(columnDefinition = "BINARY(32) NOT NULL")
  private byte[] urlHash;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 32)
  private IngestionState state;

  private ArticleIngestionJob(Article article, byte[] urlHash, IngestionState state) {
    this.article = article;
    this.urlHash = urlHash;
    this.state = state;
  }

  public static ArticleIngestionJob create(Article article, byte[] urlHash) {
    return new ArticleIngestionJob(article, urlHash, IngestionState.PENDING);
  }

  public void transitionTo(IngestionState next) {
    this.state = next;
  }
}
