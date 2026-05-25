package org.everytldr.common.domain.article;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.everytldr.TestcontainersConfig;

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
                article, "guest", SAMPLE_PASSWORD_HASH, SAMPLE_IP_HASH, "Hello"));
    Long commentId = comment.getId();

    comment.softDelete(Instant.now());
    commentRepository.saveAndFlush(comment);
    entityManager.clear();

    assertThat(commentRepository.findById(commentId)).isEmpty();
  }
}
