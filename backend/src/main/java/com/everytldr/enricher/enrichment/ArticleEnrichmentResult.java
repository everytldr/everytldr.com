package com.everytldr.enricher.enrichment;

import java.util.Optional;

public record ArticleEnrichmentResult(
    String koTitle, String koSummary, String enTitle, String enSummary, String categorySlug) {
  private static final int TITLE_MAX_LENGTH = 500;

  public Optional<String> findValidationErrorMessage() {
    Optional<String> koTitleError = findTitleErrorMessage("koTitle", koTitle);
    if (koTitleError.isPresent()) {
      return koTitleError;
    }

    Optional<String> enTitleError = findTitleErrorMessage("enTitle", enTitle);
    if (enTitleError.isPresent()) {
      return enTitleError;
    }

    Optional<String> koSummaryError = findRequiredTextErrorMessage("koSummary", koSummary);
    if (koSummaryError.isPresent()) {
      return koSummaryError;
    }

    Optional<String> enSummaryError = findRequiredTextErrorMessage("enSummary", enSummary);
    if (enSummaryError.isPresent()) {
      return enSummaryError;
    }

    return findRequiredTextErrorMessage("categorySlug", categorySlug);
  }

  private Optional<String> findTitleErrorMessage(String fieldName, String value) {
    Optional<String> requiredError = findRequiredTextErrorMessage(fieldName, value);
    if (requiredError.isPresent()) {
      return requiredError;
    }
    if (value.length() > TITLE_MAX_LENGTH) {
      return Optional.of("%s exceeds %d characters".formatted(fieldName, TITLE_MAX_LENGTH));
    }
    return Optional.empty();
  }

  private Optional<String> findRequiredTextErrorMessage(String fieldName, String value) {
    if (value == null || value.isBlank()) {
      return Optional.of("%s is blank".formatted(fieldName));
    }
    return Optional.empty();
  }
}
