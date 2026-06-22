package com.everytldr.enricher.content;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy.CrawlingPolicy;
import com.everytldr.common.domain.source.SourcePolicy.EligibilityPolicy;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailCandidateSelector;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailEligibilityPolicy;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;

public class ThumbnailEligibilityChecker {

  public Optional<String> findAllowedThumbnailUrl(
      Document document, ArticleSource source, String currentThumbnailUrl) {
    Objects.requireNonNull(document, "document must not be null");
    Objects.requireNonNull(source, "source must not be null");

    if (StringUtils.hasText(currentThumbnailUrl)) {
      return Optional.empty();
    }

    EligibilityPolicy eligibilityPolicy = source.getPolicy().eligibility();
    return switch (eligibilityPolicy.thumbnailPolicy()) {
      case ALLOW -> findUnverifiedThumbnailUrl(document, source.getPolicy().crawling());
      case DISABLED -> Optional.empty();
      case ELIGIBLE_ONLY ->
          findCreditVerifiedThumbnailUrl(document, eligibilityPolicy.thumbnailEligibility());
    };
  }

  private Optional<String> findCreditVerifiedThumbnailUrl(
      Document document, ThumbnailEligibilityPolicy policy) {
    Objects.requireNonNull(document, "document must not be null");
    Objects.requireNonNull(policy, "policy must not be null");

    for (ThumbnailCandidateSelector candidateSelector : policy.candidateSelectors()) {
      Optional<String> thumbnailUrl =
          findCreditVerifiedThumbnailUrl(document, candidateSelector, policy);
      if (thumbnailUrl.isPresent()) {
        return thumbnailUrl;
      }
    }

    return Optional.empty();
  }

  private Optional<String> findCreditVerifiedThumbnailUrl(
      Document document,
      ThumbnailCandidateSelector candidateSelector,
      ThumbnailEligibilityPolicy policy) {
    for (Element image : document.select(candidateSelector.selector())) {
      String imageUrl = resolveImageUrl(image, candidateSelector.urlAttribute());
      if (!StringUtils.hasText(imageUrl)) {
        continue;
      }

      Optional<String> creditText = findCreditText(image, candidateSelector);
      if (creditText.isEmpty()) {
        continue;
      }

      if (containsAny(creditText.get(), policy.deniedCreditFragments())) {
        continue;
      }
      if (containsAny(creditText.get(), policy.allowedCreditFragments())) {
        return Optional.of(imageUrl);
      }
    }

    return Optional.empty();
  }

  private Optional<String> findUnverifiedThumbnailUrl(Document document, CrawlingPolicy policy) {
    for (String selector : policy.thumbnailSelectors()) {
      Element image = document.selectFirst(selector);
      if (image != null) {
        String imageUrl = image.absUrl("src");
        if (StringUtils.hasText(imageUrl)) {
          return Optional.of(imageUrl);
        }
      }
    }

    Element ogImage = document.selectFirst("meta[property=\"og:image\"]");
    if (ogImage != null) {
      String ogImageUrl = ogImage.absUrl("content");
      if (StringUtils.hasText(ogImageUrl)) {
        return Optional.of(ogImageUrl);
      }
    }

    return Optional.empty();
  }

  private String resolveImageUrl(Element image, String urlAttribute) {
    String absoluteUrl = image.absUrl(urlAttribute);
    if (StringUtils.hasText(absoluteUrl)) {
      return absoluteUrl;
    }
    return image.attr(urlAttribute);
  }

  private Optional<String> findCreditText(
      Element image, ThumbnailCandidateSelector candidateSelector) {
    Element creditContainer = image.closest(candidateSelector.creditContainerSelector());
    if (creditContainer == null) {
      return Optional.empty();
    }

    String creditText =
        candidateSelector.creditSelectors().stream()
            .flatMap(selector -> creditContainer.select(selector).stream())
            .map(Element::text)
            .map(this::normalizeText)
            .filter(StringUtils::hasText)
            .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);

    return StringUtils.hasText(creditText) ? Optional.of(creditText) : Optional.empty();
  }

  private boolean containsAny(String text, Iterable<String> fragments) {
    for (String fragment : fragments) {
      if (contains(text, fragment)) {
        return true;
      }
    }
    return false;
  }

  private boolean contains(String text, String fragment) {
    return normalizeText(text)
        .toLowerCase(Locale.ROOT)
        .contains(normalizeText(fragment).toLowerCase(Locale.ROOT));
  }

  private String normalizeText(String text) {
    return text.replaceAll("\\s+", " ").trim();
  }
}
