package com.everytldr.api.article;

import com.everytldr.api.support.client.ClientAddress;
import com.everytldr.api.support.client.ResolvedClientAddress;
import com.everytldr.common.domain.article.ArticleComment;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

  @PatchMapping("/{commentId}")
  @Operation(operationId = "editArticleComment")
  public ArticleCommentListResponse.Item editComment(
      @PathVariable @Schema(type = "string") Long articleId,
      @PathVariable @Schema(type = "string") Long commentId,
      @Valid @RequestBody ArticleCommentEditRequest body) {
    ArticleComment comment =
        articleCommentService.editComment(articleId, commentId, body.password(), body.content());
    return ArticleCommentListResponse.Item.from(comment);
  }

  @DeleteMapping("/{commentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "deleteArticleComment")
  public void deleteComment(
      @PathVariable @Schema(type = "string") Long articleId,
      @PathVariable @Schema(type = "string") Long commentId,
      @Valid @RequestBody ArticleCommentPasswordRequest body) {
    articleCommentService.deleteComment(articleId, commentId, body.password());
  }

  @PostMapping("/{commentId}/password-verification")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(operationId = "verifyArticleCommentPassword")
  public void verifyPassword(
      @PathVariable @Schema(type = "string") Long articleId,
      @PathVariable @Schema(type = "string") Long commentId,
      @Valid @RequestBody ArticleCommentPasswordRequest body) {
    articleCommentService.verifyPassword(articleId, commentId, body.password());
  }

  public record ArticleCommentEditRequest(
      @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 4, max = 100)
          String password,
      @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 5000) String content) {}

  public record ArticleCommentPasswordRequest(
      @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 4, max = 100)
          String password) {}

  public record ArticleCommentCreateRequest(
      @Schema(
              requiredMode = RequiredMode.REQUIRED,
              types = {"string", "null"})
          String parentId,
      @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 50) String nickname,
      @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(min = 4, max = 100)
          String password,
      @Schema(requiredMode = RequiredMode.REQUIRED) @NotBlank @Size(max = 5000) String content) {}

  public record ArticleCommentListResponse(
      @Schema(requiredMode = RequiredMode.REQUIRED) List<Item> items) {
    @Schema(name = "ArticleCommentListItem")
    public record Item(
        @Schema(requiredMode = RequiredMode.REQUIRED) String id,
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                types = {"string", "null"})
            String parentId,
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                types = {"string", "null"})
            String nickname,
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                types = {"string", "null"})
            String maskedIp,
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                types = {"string", "null"})
            String content,
        @Schema(requiredMode = RequiredMode.REQUIRED) Instant createdAt,
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                types = {"string", "null"})
            Instant editedAt,
        @Schema(
                requiredMode = RequiredMode.REQUIRED,
                types = {"string", "null"})
            Instant deletedAt) {
      public static Item from(ArticleComment comment) {
        Long parentId = comment.getParent() == null ? null : comment.getParent().getId();
        boolean deleted = comment.isSoftDeleted();
        return new Item(
            comment.getId().toString(),
            parentId == null ? null : parentId.toString(),
            deleted ? null : comment.getNickname(),
            deleted ? null : comment.getMaskedIp(),
            deleted ? null : comment.getContent(),
            comment.getCreatedAt(),
            deleted ? null : comment.getEditedAt(),
            comment.getDeletedAt());
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
