package com.everytldr.common.domain.source;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public record SourcePolicy(CrawlingPolicy crawling, EligibilityPolicy eligibility) {
  public SourcePolicy(CrawlingPolicy crawling) {
    this(crawling, null);
  }

  public SourcePolicy {
    Objects.requireNonNull(crawling, "crawling must not be null");
    eligibility = Objects.requireNonNullElse(eligibility, EligibilityPolicy.none());
  }

  public record CrawlingPolicy(
      @JsonProperty("feed_urls") List<String> feedUrls,
      List<String> hosts,
      @JsonProperty("content_selectors") List<String> contentSelectors,
      @JsonProperty("thumbnail_selectors") List<String> thumbnailSelectors) {
    public CrawlingPolicy {
      if (feedUrls == null || feedUrls.isEmpty()) {
        throw new IllegalArgumentException("feedUrls must not be empty");
      }
      if (hosts == null || hosts.isEmpty()) {
        throw new IllegalArgumentException("hosts must not be empty");
      }
      if (contentSelectors == null || contentSelectors.isEmpty()) {
        throw new IllegalArgumentException("contentSelectors must not be empty");
      }
      feedUrls = List.copyOf(feedUrls);
      hosts = List.copyOf(hosts);
      contentSelectors = List.copyOf(contentSelectors);
      thumbnailSelectors = List.copyOf(Objects.requireNonNullElse(thumbnailSelectors, List.of()));
    }

    public boolean isAllowedHost(String host) {
      if (host == null || host.isBlank()) {
        return false;
      }
      return hosts.stream().anyMatch(allowed -> allowed.equalsIgnoreCase(host));
    }
  }

  public record EligibilityPolicy(
      @JsonProperty("article_rules") List<ArticleEligibilityRule> articleRules,
      @JsonProperty("thumbnail_policy") ThumbnailPolicy thumbnailPolicy,
      @JsonProperty("thumbnail_eligibility") ThumbnailEligibilityPolicy thumbnailEligibility) {
    public EligibilityPolicy {
      articleRules = copyPolicies(articleRules, "articleRules");
      thumbnailPolicy = Objects.requireNonNullElse(thumbnailPolicy, ThumbnailPolicy.ALLOW);

      if (thumbnailPolicy == ThumbnailPolicy.ELIGIBLE_ONLY && thumbnailEligibility == null) {
        throw new IllegalArgumentException(
            "thumbnailEligibility must be configured when thumbnailPolicy is ELIGIBLE_ONLY");
      }
    }

    public static EligibilityPolicy none() {
      return new EligibilityPolicy(List.of(), ThumbnailPolicy.ALLOW, null);
    }

    @JsonIgnore
    public boolean hasArticleEligibilityRules() {
      return !articleRules.isEmpty();
    }

    @JsonIgnore
    public boolean isThumbnailDisabled() {
      return thumbnailPolicy == ThumbnailPolicy.DISABLED;
    }
  }

  public record ArticleEligibilityRule(
      RuleType type, String selector, String attribute, List<String> values) {
    public ArticleEligibilityRule {
      Objects.requireNonNull(type, "type must not be null");
      selector = normalizeOptionalText(selector);
      attribute = normalizeOptionalText(attribute);
      values = copyPolicyValues(values, "values");

      switch (type) {
        case SELECTOR_EXISTS -> requireText(selector, "selector");
        case SELECTOR_TEXT_CONTAINS_ANY, SELECTOR_TEXT_NOT_EQUALS_ANY -> {
          requireText(selector, "selector");
          requireValues(values, "values");
        }
        case SELECTOR_ATTRIBUTE_PREFIX_ANY -> {
          requireText(selector, "selector");
          requireText(attribute, "attribute");
          requireValues(values, "values");
        }
        case DOCUMENT_HTML_CONTAINS_ANY, DOCUMENT_HTML_NOT_CONTAINS_ANY ->
            requireValues(values, "values");
      }
    }
  }

  public enum RuleType {
    SELECTOR_EXISTS,
    SELECTOR_TEXT_CONTAINS_ANY,
    SELECTOR_TEXT_NOT_EQUALS_ANY,
    SELECTOR_ATTRIBUTE_PREFIX_ANY,
    DOCUMENT_HTML_CONTAINS_ANY,
    DOCUMENT_HTML_NOT_CONTAINS_ANY
  }

  public record ThumbnailEligibilityPolicy(
      @JsonProperty("candidate_selectors") List<ThumbnailCandidateSelector> candidateSelectors,
      @JsonProperty("allowed_credit_fragments") List<String> allowedCreditFragments,
      @JsonProperty("denied_credit_fragments") List<String> deniedCreditFragments) {
    public ThumbnailEligibilityPolicy {
      candidateSelectors = copyPolicies(candidateSelectors, "candidateSelectors");
      if (candidateSelectors.isEmpty()) {
        throw new IllegalArgumentException("candidateSelectors must not be empty");
      }
      allowedCreditFragments = copyPolicyValues(allowedCreditFragments, "allowedCreditFragments");
      deniedCreditFragments = copyPolicyValues(deniedCreditFragments, "deniedCreditFragments");
      requireValues(allowedCreditFragments, "allowedCreditFragments");
    }
  }

  public record ThumbnailCandidateSelector(
      String selector,
      @JsonProperty("url_attribute") String urlAttribute,
      @JsonProperty("credit_container_selector") String creditContainerSelector,
      @JsonProperty("credit_selectors") List<String> creditSelectors) {
    public ThumbnailCandidateSelector {
      selector = normalizeOptionalText(selector);
      urlAttribute = normalizeOptionalText(urlAttribute);
      creditContainerSelector = normalizeOptionalText(creditContainerSelector);
      creditSelectors = copyPolicyValues(creditSelectors, "creditSelectors");

      requireText(selector, "selector");
      requireText(urlAttribute, "urlAttribute");
      requireText(creditContainerSelector, "creditContainerSelector");
      requireValues(creditSelectors, "creditSelectors");
    }
  }

  public enum ThumbnailPolicy {
    ALLOW,
    DISABLED,
    ELIGIBLE_ONLY
  }

  private static <T> List<T> copyPolicies(List<T> values, String fieldName) {
    if (values == null) {
      return List.of();
    }
    if (values.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(fieldName + " must not contain null values");
    }
    return List.copyOf(values);
  }

  private static List<String> copyPolicyValues(List<String> values, String fieldName) {
    if (values == null) {
      return List.of();
    }
    if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
      throw new IllegalArgumentException(fieldName + " must not contain blank values");
    }
    return values.stream().map(String::trim).toList();
  }

  private static void requireText(String value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + " must not be blank");
    }
  }

  private static void requireValues(Collection<String> values, String fieldName) {
    if (values.isEmpty()) {
      throw new IllegalArgumentException(fieldName + " must not be empty");
    }
  }

  private static String normalizeOptionalText(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
