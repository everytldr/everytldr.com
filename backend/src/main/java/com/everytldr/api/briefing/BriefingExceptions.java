package com.everytldr.api.briefing;

import java.time.LocalDate;

public final class BriefingExceptions {
  private BriefingExceptions() {}

  public static class NotFound extends RuntimeException {
    public NotFound(LocalDate briefingDate) {
      super("briefing not found: " + briefingDate);
    }

    public NotFound(Long articleId) {
      super("no briefing covers article: " + articleId);
    }
  }
}
