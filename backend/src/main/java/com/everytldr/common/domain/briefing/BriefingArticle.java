package com.everytldr.common.domain.briefing;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_briefing_article_date_article",
            columnNames = {"briefing_date", "article_id"}))
public class BriefingArticle extends BaseEntity {
  @Column(nullable = false)
  private LocalDate briefingDate;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "article_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_briefing_article_article"))
  private Article article;

  private BriefingArticle(LocalDate briefingDate, Article article) {
    this.briefingDate = briefingDate;
    this.article = article;
  }

  public static BriefingArticle create(LocalDate briefingDate, Article article) {
    return new BriefingArticle(briefingDate, article);
  }
}
