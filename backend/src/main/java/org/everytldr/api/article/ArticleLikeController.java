package org.everytldr.api.article;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.everytldr.api.support.client.ClientAddress;
import org.everytldr.api.support.client.ResolvedClientAddress;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles/{articleId}/likes/me")
@Profile("api")
@Tag(name = "Articles")
public class ArticleLikeController {
  private final ArticleLikeService articleLikeService;

  public ArticleLikeController(ArticleLikeService articleLikeService) {
    this.articleLikeService = articleLikeService;
  }

  @GetMapping
  @Operation(operationId = "getMyArticleLike")
  public ArticleLikeStateResponse getMyLike(
      @PathVariable Long articleId,
      @Parameter(hidden = true) @ResolvedClientAddress ClientAddress clientAddress) {
    ArticleLikeService.LikeState state =
        articleLikeService.getLikeState(articleId, clientAddress.ipHash());
    return ArticleLikeStateResponse.from(state);
  }

  @PutMapping
  @Operation(operationId = "likeArticle")
  public ArticleLikeStateResponse like(
      @PathVariable Long articleId,
      @Parameter(hidden = true) @ResolvedClientAddress ClientAddress clientAddress) {
    ArticleLikeService.LikeState state = articleLikeService.like(articleId, clientAddress.ipHash());
    return ArticleLikeStateResponse.from(state);
  }

  @DeleteMapping
  @Operation(operationId = "unlikeArticle")
  public ArticleLikeStateResponse unlike(
      @PathVariable Long articleId,
      @Parameter(hidden = true) @ResolvedClientAddress ClientAddress clientAddress) {
    ArticleLikeService.LikeState state =
        articleLikeService.unlike(articleId, clientAddress.ipHash());
    return ArticleLikeStateResponse.from(state);
  }

  public record ArticleLikeStateResponse(Long articleId, boolean likedByReader, long likeCount) {
    public static ArticleLikeStateResponse from(ArticleLikeService.LikeState state) {
      return new ArticleLikeStateResponse(
          state.articleId(), state.likedByReader(), state.likeCount());
    }
  }
}
