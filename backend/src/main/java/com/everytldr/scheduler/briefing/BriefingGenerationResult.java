package com.everytldr.scheduler.briefing;

public record BriefingGenerationResult(String language, String title, String content) {
  private static final int TITLE_MAX_LENGTH = 500;
  private static final String INVALID_PREFIX = "invalid briefing generation result: ";

  public void assertValid() {
    assertRequiredText("language", language);
    assertRequiredText("title", title);
    if (title.length() > TITLE_MAX_LENGTH) {
      throw new BriefingGenerationException(
          INVALID_PREFIX + "title exceeds %d characters".formatted(TITLE_MAX_LENGTH));
    }
    assertRequiredText("content", content);
  }

  private void assertRequiredText(String fieldName, String value) {
    if (value == null || value.isBlank()) {
      throw new BriefingGenerationException(INVALID_PREFIX + "%s is blank".formatted(fieldName));
    }
  }
}
