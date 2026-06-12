package com.everytldr.enricher.enrichment;

public record EnrichmentResult(String language, String title, String summary, String categorySlug) {
  private static final int TITLE_MAX_LENGTH = 500;
  private static final String INVALID_PREFIX = "invalid enrichment result: ";

  public void assertValid() {
    assertRequiredText("language", language);
    assertRequiredText("title", title);
    if (title.length() > TITLE_MAX_LENGTH) {
      throw EnrichmentException.permanent(
          INVALID_PREFIX + "title exceeds %d characters".formatted(TITLE_MAX_LENGTH));
    }
    assertRequiredText("summary", summary);
    assertRequiredText("categorySlug", categorySlug);
  }

  private void assertRequiredText(String fieldName, String value) {
    if (value == null || value.isBlank()) {
      throw EnrichmentException.permanent(INVALID_PREFIX + "%s is blank".formatted(fieldName));
    }
  }
}
