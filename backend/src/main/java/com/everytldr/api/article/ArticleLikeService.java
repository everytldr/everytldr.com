package com.everytldr.api.article;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleLike;
import com.everytldr.common.domain.article.ArticleLikeRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("api")
public class ArticleLikeService {
  private final ArticleService articleService;
  private final ArticleLikeRepository likeRepository;

  public ArticleLikeService(ArticleService articleService, ArticleLikeRepository likeRepository) {
    this.articleService = articleService;
    this.likeRepository = likeRepository;
  }

  public LikeState getLikeState(Long articleId, String ipHash) {
    articleService.assertArticleExists(articleId);
    boolean liked =
        likeRepository
            .findByArticleIdAndIpHash(articleId, ipHash)
            .map(ArticleLike::isActive)
            .orElse(false);
    return new LikeState(
        articleId, liked, likeRepository.countByArticleIdAndIsActiveTrue(articleId));
  }

  @Transactional
  public LikeState like(Long articleId, String ipHash) {
    Article article = articleService.getArticleOrThrow(articleId);
    ArticleLike like =
        likeRepository
            .findByArticleIdAndIpHash(articleId, ipHash)
            .orElseGet(() -> likeRepository.save(ArticleLike.create(article, ipHash)));
    like.activate();
    return new LikeState(
        articleId, true, likeRepository.countByArticleIdAndIsActiveTrue(articleId));
  }

  @Transactional
  public LikeState unlike(Long articleId, String ipHash) {
    articleService.assertArticleExists(articleId);
    likeRepository.findByArticleIdAndIpHash(articleId, ipHash).ifPresent(ArticleLike::deactivate);
    return new LikeState(
        articleId, false, likeRepository.countByArticleIdAndIsActiveTrue(articleId));
  }

  public record LikeState(Long articleId, boolean likedByReader, long likeCount) {}
}
