package com.everytldr.scheduler.briefing;

import java.util.List;

public interface BriefingGenerationClient {
  List<Result> generate(Request request);

  record Request(List<SourceArticle> articles) {
    public Request {
      if (articles == null || articles.isEmpty()) {
        throw new IllegalArgumentException("articles must not be empty");
      }
    }

    public record SourceArticle(String title, String summary) {
      public SourceArticle {
        if (title == null || title.isBlank()) {
          throw new IllegalArgumentException("title must not be blank");
        }
        if (summary == null || summary.isBlank()) {
          throw new IllegalArgumentException("summary must not be blank");
        }
      }
    }
  }

  record Result(String language, String title, String content) {
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
}
