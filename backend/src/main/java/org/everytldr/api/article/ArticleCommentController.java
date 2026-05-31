package org.everytldr.api.article;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.everytldr.api.support.client.ClientAddress;
import org.everytldr.api.support.client.ResolvedClientAddress;
import org.everytldr.common.domain.article.ArticleComment;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/articles/{articleId}/comments")
@Profile("api")
@Tag(name = "Articles")
public class ArticleCommentController {
  private final ArticleCommentService articleCommentService;

  public ArticleCommentController(ArticleCommentService articleCommentService) {
    this.articleCommentService = articleCommentService;
  }

  @GetMapping
  @Operation(operationId = "listArticleComments")
  public ArticleCommentListResponse comments(
      @PathVariable @Schema(type = "string") Long articleId) {
    List<ArticleCommentListResponse.Item> items =
        articleCommentService.listComments(articleId).stream()
            .map(ArticleCommentListResponse.Item::from)
            .toList();
    return new ArticleCommentListResponse(items);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(operationId = "createArticleComment")
  public ArticleCommentListResponse.Item createComment(
      @PathVariable @Schema(type = "string") Long articleId,
      @Valid @RequestBody ArticleCommentCreateRequest body,
      @Parameter(hidden = true) @ResolvedClientAddress ClientAddress clientAddress) {
    ArticleComment comment =
        articleCommentService.createComment(
            articleId,
            parseParentId(body.parentId()).orElse(null),
            body.nickname(),
            body.password(),
            body.content(),
            clientAddress.ipHash(),
            clientAddress.maskedIp());
    return ArticleCommentListResponse.Item.from(comment);
  }

  public record ArticleCommentCreateRequest(
      String parentId,
      @NotBlank @Size(max = 50) String nickname,
      @NotBlank @Size(min = 4, max = 100) String password,
      @NotBlank @Size(max = 5000) String content) {}

  public record ArticleCommentListResponse(List<Item> items) {
    @Schema(name = "ArticleCommentListItem")
    public record Item(
        String id,
        String parentId,
        String nickname,
        String maskedIp,
        String content,
        Instant createdAt) {
      public static Item from(ArticleComment comment) {
        Long parentId = comment.getParent() == null ? null : comment.getParent().getId();
        return new Item(
            comment.getId().toString(),
            parentId == null ? null : parentId.toString(),
            comment.getNickname(),
            comment.getMaskedIp(),
            comment.getContent(),
            comment.getCreatedAt());
      }
    }
  }

  private static Optional<Long> parseParentId(String parentId) {
    if (parentId == null) {
      return Optional.empty();
    }

    try {
      return Optional.of(Long.parseLong(parentId));
    } catch (NumberFormatException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid parent id", e);
    }
  }
}
