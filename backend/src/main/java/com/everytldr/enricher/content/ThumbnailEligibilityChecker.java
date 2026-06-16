package com.everytldr.enricher.content;

import com.everytldr.common.domain.source.SourcePolicy.ThumbnailCandidateSelector;
import com.everytldr.common.domain.source.SourcePolicy.ThumbnailEligibilityPolicy;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.StringUtils;

public class ThumbnailEligibilityChecker {

  public Optional<String> findEligibleThumbnailUrl(
      Document document, ThumbnailEligibilityPolicy policy) {
    Objects.requireNonNull(document, "document must not be null");
    Objects.requireNonNull(policy, "policy must not be null");

    for (ThumbnailCandidateSelector candidateSelector : policy.candidateSelectors()) {
      Optional<String> thumbnailUrl = findEligibleThumbnailUrl(document, candidateSelector, policy);
      if (thumbnailUrl.isPresent()) {
        return thumbnailUrl;
      }
    }

    return Optional.empty();
  }

  private Optional<String> findEligibleThumbnailUrl(
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
