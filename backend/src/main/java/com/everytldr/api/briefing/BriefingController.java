package com.everytldr.api.briefing;

import com.everytldr.api.article.ArticleController.ArticleListResponse;
import com.everytldr.api.article.ArticleService;
import com.everytldr.api.support.language.ResolvedLanguage;
import com.everytldr.api.support.pagination.Pagination;
import com.everytldr.common.domain.article.ArticleRepository.ListItemProjection;
import com.everytldr.common.domain.briefing.Briefing;
import com.everytldr.common.domain.language.SupportedLanguage;
import com.everytldr.common.domain.license.LicensePolicyEvaluator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/briefings")
@Profile("api")
@Tag(name = "Briefings")
@RequiredArgsConstructor
public class BriefingController {
  private static final int EXCERPT_MAX_LENGTH = 200;

  private final BriefingService briefingService;
  private final ArticleService articleService;
  private final LicensePolicyEvaluator licensePolicyEvaluator;

  @GetMapping
  @Operation(operationId = "listBriefings")
  public BriefingListResponse list(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate cursor,
      @RequestParam(required = false) Integer size) {
    int pageSize = Pagination.clampSize(size);
    BriefingService.ListResult result = briefingService.listRecent(language, cursor, pageSize);
    return BriefingListResponse.from(result);
  }

  @GetMapping("/{date}")
  @Operation(operationId = "getBriefing")
  public BriefingDetailResponse get(
      @Parameter(hidden = true) @ResolvedLanguage SupportedLanguage language,
      @PathVariable @DateTimeFormat(iso = ISO.DATE) LocalDate date) {
    Briefing briefing = briefingService.getBriefing(language, date);
    List<Long> articleIds = briefingService.listArticleIds(date);
    List<ListItemProjection> articles =
        articleService.listByIdsInOrder(language, articleIds, articleIds.size());
    BriefingService.AdjacentDates adjacentDates = briefingService.findAdjacentDates(language, date);
    return BriefingDetailResponse.from(briefing, articles, adjacentDates, licensePolicyEvaluator);
  }

  public record BriefingListResponse(
      @Schema(requiredMode = RequiredMode.REQUIRED) List<Item> items,
      @Schema(
              requiredMode = RequiredMode.REQUIRED,
              types = {"string", "null"},
              format = "date")
          LocalDate nextCursor) {
    static BriefingListResponse from(BriefingService.ListResult result) {
      List<Item> items = result.items().stream().map(Item::from).toList();
      return new BriefingListResponse(items, result.nextCursor());
    }

    @Schema(name = "BriefingListItem")
    public record Item(
        @Schema(requiredMode = RequiredMode.REQUIRED) LocalDate date,
        @Schema(requiredMode = RequiredMode.REQUIRED) String title,
        @Schema(requiredMode = RequiredMode.REQUIRED) String excerpt) {
      static Item from(Briefing briefing) {
        return new Item(
            briefing.getBriefingDate(),
            briefing.getTitle(),
            briefing.extractExcerpt(EXCERPT_MAX_LENGTH));
      }
    }
  }

  public record BriefingDetailResponse(
      @Schema(requiredMode = RequiredMode.REQUIRED) LocalDate date,
      @Schema(requiredMode = RequiredMode.REQUIRED) String title,
      @Schema(requiredMode = RequiredMode.REQUIRED) String content,
      @Schema(requiredMode = RequiredMode.REQUIRED) boolean requiresShareAlike,
      @Schema(requiredMode = RequiredMode.REQUIRED) List<ArticleListResponse.Item> articles,
      @Schema(
              requiredMode = RequiredMode.REQUIRED,
              types = {"string", "null"},
              format = "date")
          LocalDate previousDate,
      @Schema(
              requiredMode = RequiredMode.REQUIRED,
              types = {"string", "null"},
              format = "date")
          LocalDate nextDate) {
    static BriefingDetailResponse from(
        Briefing briefing,
        List<ListItemProjection> articles,
        BriefingService.AdjacentDates adjacentDates,
        LicensePolicyEvaluator licensePolicyEvaluator) {
      List<ArticleListResponse.Item> items =
          articles.stream()
              .map(item -> ArticleListResponse.Item.from(item, licensePolicyEvaluator))
              .toList();
      boolean requiresShareAlike =
          articles.stream()
              .anyMatch(
                  article -> licensePolicyEvaluator.requiresShareAlike(article.licenseInfo()));
      return new BriefingDetailResponse(
          briefing.getBriefingDate(),
          briefing.getTitle(),
          briefing.getContent(),
          requiresShareAlike,
          items,
          adjacentDates.previousDate(),
          adjacentDates.nextDate());
    }
  }
}
