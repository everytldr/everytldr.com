package com.everytldr.enricher.enrichment;

import java.util.Optional;

public record ArticleEnrichmentResult(
    String koTitle, String koSummary, String enTitle, String enSummary, String categorySlug) {
  private static final int TITLE_MAX_LENGTH = 500;

  public boolean isValid() {
    return validationErrorMessage().isEmpty();
  }

  public Optional<String> validationErrorMessage() {
    Optional<String> koTitleError = validateTitle("koTitle", koTitle);
    if (koTitleError.isPresent()) {
      return koTitleError;
    }

    Optional<String> enTitleError = validateTitle("enTitle", enTitle);
    if (enTitleError.isPresent()) {
      return enTitleError;
    }

    Optional<String> koSummaryError = validateRequiredText("koSummary", koSummary);
    if (koSummaryError.isPresent()) {
      return koSummaryError;
    }

    Optional<String> enSummaryError = validateRequiredText("enSummary", enSummary);
    if (enSummaryError.isPresent()) {
      return enSummaryError;
    }

    return validateRequiredText("categorySlug", categorySlug);
  }

  private Optional<String> validateTitle(String fieldName, String value) {
    Optional<String> requiredError = validateRequiredText(fieldName, value);
    if (requiredError.isPresent()) {
      return requiredError;
    }
    if (value.length() > TITLE_MAX_LENGTH) {
      return Optional.of("%s exceeds %d characters".formatted(fieldName, TITLE_MAX_LENGTH));
    }
    return Optional.empty();
  }

  private Optional<String> validateRequiredText(String fieldName, String value) {
    if (value == null || value.isBlank()) {
      return Optional.of("%s is blank".formatted(fieldName));
    }
    return Optional.empty();
  }
}
