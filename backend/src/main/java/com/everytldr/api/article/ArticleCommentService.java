package com.everytldr.api.article;

import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleComment;
import com.everytldr.common.domain.article.ArticleCommentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("api")
public class ArticleCommentService {
  private final ArticleService articleService;
  private final ArticleCommentRepository commentRepository;

  public ArticleCommentService(
      ArticleService articleService, ArticleCommentRepository commentRepository) {
    this.articleService = articleService;
    this.commentRepository = commentRepository;
  }

  public List<ArticleComment> listComments(Long articleId) {
    articleService.assertArticleExists(articleId);
    return commentRepository.findThreadByArticleId(articleId);
  }

  @Transactional
  public ArticleComment createComment(
      Long articleId,
      Long parentId,
      String nickname,
      String password,
      String content,
      String ipHash,
      String maskedIp) {
    Article article = articleService.getArticleOrThrow(articleId);
    ArticleComment parent = null;
    if (parentId != null) {
      parent =
          commentRepository
              .findByIdAndArticleId(parentId, articleId)
              .orElseThrow(() -> new ArticleCommentExceptions.InvalidParent(parentId));
    }

    String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
    ArticleComment comment =
        parent == null
            ? ArticleComment.createTopLevel(
                article, nickname, passwordHash, ipHash, maskedIp, content)
            : ArticleComment.createReply(
                article, parent, nickname, passwordHash, ipHash, maskedIp, content);
    return commentRepository.save(comment);
  }

  public void verifyPassword(Long articleId, Long commentId, String password) {
    ArticleComment comment = getCommentOrThrow(articleId, commentId);
    assertPasswordMatches(comment, password);
  }

  @Transactional
  public ArticleComment editComment(
      Long articleId, Long commentId, String password, String content) {
    ArticleComment comment = getCommentOrThrow(articleId, commentId);
    assertPasswordMatches(comment, password);
    comment.edit(content, Instant.now());
    return comment;
  }

  @Transactional
  public void deleteComment(Long articleId, Long commentId, String password) {
    ArticleComment comment = getCommentOrThrow(articleId, commentId);
    assertPasswordMatches(comment, password);
    comment.softDelete(Instant.now());
  }

  private ArticleComment getCommentOrThrow(Long articleId, Long commentId) {
    articleService.assertArticleExists(articleId);
    return commentRepository
        .findByIdAndArticleId(commentId, articleId)
        .orElseThrow(() -> new ArticleCommentExceptions.NotFound(commentId));
  }

  private void assertPasswordMatches(ArticleComment comment, String password) {
    if (!BCrypt.checkpw(password, comment.getPasswordHash())) {
      throw new ArticleCommentExceptions.PasswordMismatch(comment.getId());
    }
  }
}
