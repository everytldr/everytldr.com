package com.everytldr.common.domain.article;

import static org.assertj.core.api.Assertions.assertThat;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.ArticleSourceRepository;
import com.everytldr.common.domain.source.SourcePolicy;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourceType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class SoftDeleteTest {
  private static final String SAMPLE_PASSWORD_HASH = "$2a$10$test";
  private static final String SAMPLE_IP_HASH = "0123456789abcdef";

  @PersistenceContext private EntityManager entityManager;

  @Autowired private ArticleRepository articleRepository;

  @Autowired private ArticleCommentRepository commentRepository;

  @Autowired private ArticleSourceRepository sourceRepository;

  @BeforeEach
  void seedSource() {
    sourceRepository.saveAndFlush(source());
  }

  @Test
  void softDeletedArticleIsHiddenFromFindById() {
    Article article =
        articleRepository.saveAndFlush(
            Article.create("https://example.com/news/1", "Example", null, "en", Instant.now()));
    Long id = article.getId();

    article.softDelete(Instant.now());
    articleRepository.saveAndFlush(article);
    entityManager.clear();

    assertThat(articleRepository.findById(id)).isEmpty();
  }

  @Test
  void softDeletedArticleIsHiddenFromFindAll() {
    Article kept =
        articleRepository.saveAndFlush(
            Article.create("https://example.com/keep", "Example", null, "en", Instant.now()));
    Article deleted =
        articleRepository.saveAndFlush(
            Article.create("https://example.com/delete", "Example", null, "en", Instant.now()));
    Long keptId = kept.getId();
    Long deletedId = deleted.getId();

    deleted.softDelete(Instant.now());
    articleRepository.saveAndFlush(deleted);
    entityManager.clear();

    assertThat(articleRepository.findAll())
        .extracting(Article::getId)
        .contains(keptId)
        .doesNotContain(deletedId);
  }

  @Test
  void softDeletedCommentIsHiddenFromFindById() {
    Article article =
        articleRepository.saveAndFlush(
            Article.create("https://example.com/news/2", "Example", null, "en", Instant.now()));

    ArticleComment comment =
        commentRepository.saveAndFlush(
            ArticleComment.createTopLevel(
                article, "guest", SAMPLE_PASSWORD_HASH, SAMPLE_IP_HASH, "203.0", "Hello"));
    Long commentId = comment.getId();

    comment.softDelete(Instant.now());
    commentRepository.saveAndFlush(comment);
    entityManager.clear();

    assertThat(commentRepository.findById(commentId)).isEmpty();
  }

  private ArticleSource source() {
    return ArticleSource.create(
        "Example",
        new SourcePolicy(
            new CrawlingPolicy(
                List.of("https://example.com/feed.xml"),
                List.of("example.com"),
                List.of("article"),
                List.of())),
        "en",
        SourceType.RSS);
  }
}
