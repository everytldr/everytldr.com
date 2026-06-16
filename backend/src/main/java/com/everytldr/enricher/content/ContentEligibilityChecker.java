package com.everytldr.enricher.content;

import com.everytldr.common.domain.source.ArticleSource;
import com.everytldr.common.domain.source.SourcePolicy.ArticleEligibilityRule;
import com.everytldr.common.domain.source.SourcePolicy.EligibilityPolicy;
import com.everytldr.enricher.enrichment.EnrichmentException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ContentEligibilityChecker {

  public void assertEligible(Document document, ArticleSource source) {
    Objects.requireNonNull(document, "document must not be null");
    Objects.requireNonNull(source, "source must not be null");

    EligibilityPolicy policy = source.getPolicy().eligibility();
    Optional<String> errorMessage = findEligibilityErrorMessage(document, policy);
    if (errorMessage.isPresent()) {
      throw EnrichmentException.permanent(
          "article failed source eligibility policy: " + errorMessage.get());
    }
  }

  private Optional<String> findEligibilityErrorMessage(
      Document document, EligibilityPolicy policy) {
    if (!policy.hasArticleEligibilityRules()) {
      return Optional.empty();
    }

    for (ArticleEligibilityRule rule : policy.articleRules()) {
      Optional<String> errorMessage = findRuleErrorMessage(document, rule);
      if (errorMessage.isPresent()) {
        return errorMessage;
      }
    }

    return Optional.empty();
  }

  private Optional<String> findRuleErrorMessage(Document document, ArticleEligibilityRule rule) {
    return switch (rule.type()) {
      case SELECTOR_EXISTS -> findSelectorExistsErrorMessage(document, rule);
      case SELECTOR_TEXT_CONTAINS_ANY -> findSelectorTextContainsAnyErrorMessage(document, rule);
      case SELECTOR_TEXT_NOT_EQUALS_ANY -> findSelectorTextNotEqualsAnyErrorMessage(document, rule);
      case SELECTOR_ATTRIBUTE_PREFIX_ANY ->
          findSelectorAttributePrefixAnyErrorMessage(document, rule);
      case DOCUMENT_HTML_CONTAINS_ANY -> findDocumentHtmlContainsAnyErrorMessage(document, rule);
      case DOCUMENT_HTML_NOT_CONTAINS_ANY ->
          findDocumentHtmlNotContainsAnyErrorMessage(document, rule);
    };
  }

  private Optional<String> findSelectorExistsErrorMessage(
      Document document, ArticleEligibilityRule rule) {
    if (document.select(rule.selector()).isEmpty()) {
      return Optional.of("required selector is missing: " + rule.selector());
    }
    return Optional.empty();
  }

  private Optional<String> findSelectorTextContainsAnyErrorMessage(
      Document document, ArticleEligibilityRule rule) {
    Elements elements = document.select(rule.selector());
    boolean hasRequiredText =
        elements.stream()
            .map(element -> normalizeText(element.text()))
            .anyMatch(text -> containsAny(text, rule.values()));

    if (!hasRequiredText) {
      return Optional.of("selector text is missing required value: " + rule.selector());
    }
    return Optional.empty();
  }

  private Optional<String> findSelectorTextNotEqualsAnyErrorMessage(
      Document document, ArticleEligibilityRule rule) {
    for (Element element : document.select(rule.selector())) {
      String text = normalizeText(element.text());
      Optional<String> deniedValue =
          rule.values().stream()
              .map(this::normalizeText)
              .filter(value -> value.equalsIgnoreCase(text))
              .findFirst();
      if (deniedValue.isPresent()) {
        return Optional.of("selector text matched denied value: " + deniedValue.get());
      }
    }
    return Optional.empty();
  }

  private Optional<String> findSelectorAttributePrefixAnyErrorMessage(
      Document document, ArticleEligibilityRule rule) {
    boolean hasAllowedAttribute =
        document.select(rule.selector()).stream()
            .anyMatch(element -> hasAllowedAttributePrefix(element, rule));

    if (!hasAllowedAttribute) {
      return Optional.of("selector attribute is missing required prefix: " + rule.selector());
    }
    return Optional.empty();
  }

  private boolean hasAllowedAttributePrefix(Element element, ArticleEligibilityRule rule) {
    String attributeValue = element.attr(rule.attribute());
    String absoluteAttributeValue = element.absUrl(rule.attribute());
    return rule.values().stream()
        .anyMatch(
            prefix ->
                attributeValue.startsWith(prefix) || absoluteAttributeValue.startsWith(prefix));
  }

  private Optional<String> findDocumentHtmlContainsAnyErrorMessage(
      Document document, ArticleEligibilityRule rule) {
    String documentHtml = normalizeText(document.outerHtml());
    if (!containsAny(documentHtml, rule.values())) {
      return Optional.of("document html is missing required value");
    }
    return Optional.empty();
  }

  private Optional<String> findDocumentHtmlNotContainsAnyErrorMessage(
      Document document, ArticleEligibilityRule rule) {
    String documentHtml = normalizeText(document.outerHtml());
    return rule.values().stream()
        .filter(value -> contains(documentHtml, value))
        .findFirst()
        .map(value -> "document html contains denied value: " + value);
  }

  private boolean containsAny(String text, Iterable<String> values) {
    for (String value : values) {
      if (contains(text, value)) {
        return true;
      }
    }
    return false;
  }

  private boolean contains(String text, String value) {
    return normalizeText(text)
        .toLowerCase(Locale.ROOT)
        .contains(normalizeText(value).toLowerCase(Locale.ROOT));
  }

  private String normalizeText(String text) {
    return text.replaceAll("\\s+", " ").trim();
  }
}
