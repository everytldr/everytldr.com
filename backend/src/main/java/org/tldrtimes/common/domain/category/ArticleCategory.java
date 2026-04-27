package org.tldrtimes.common.domain.category;

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
import org.tldrtimes.common.domain.article.Article;
import org.tldrtimes.common.domain.support.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_article_category_article_category",
        columnNames = {"article_id", "category_id"}))
public class ArticleCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "article_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_article_category_article"))
    private Article article;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "category_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_article_category_category"))
    private Category category;

    private ArticleCategory(Article article, Category category) {
        this.article = article;
        this.category = category;
    }

    public static ArticleCategory create(Article article, Category category) {
        return new ArticleCategory(article, category);
    }
}
