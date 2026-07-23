package com.everytldr.scheduler.briefing;

import java.util.List;

public record BriefingGenerationRequest(List<SourceArticle> articles) {
  public BriefingGenerationRequest {
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
