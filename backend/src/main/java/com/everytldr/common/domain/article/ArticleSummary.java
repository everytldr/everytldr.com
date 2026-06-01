package com.everytldr.common.domain.article;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.everytldr.common.domain.support.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_article_summary_article_language",
            columnNames = {"article_id", "language"}))
public class ArticleSummary extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "article_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_article_summary_article"))
  private Article article;

  @Column(nullable = false, length = 10)
  private String language;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(columnDefinition = "TEXT NOT NULL")
  private String content;

  private ArticleSummary(Article article, String language, String title, String content) {
    this.article = article;
    this.language = language;
    this.title = title;
    this.content = content;
  }

  public static ArticleSummary create(
      Article article, String language, String title, String content) {
    return new ArticleSummary(article, language, title, content);
  }

  public void rewrite(String title, String content) {
    this.title = title;
    this.content = content;
  }
}
