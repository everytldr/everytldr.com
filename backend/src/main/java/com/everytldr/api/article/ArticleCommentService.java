package com.everytldr.api.article;

import java.util.List;
import com.everytldr.common.domain.article.Article;
import com.everytldr.common.domain.article.ArticleComment;
import com.everytldr.common.domain.article.ArticleCommentRepository;
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
    return commentRepository.findByArticleIdOrderByIdAsc(articleId);
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
}
