package com.everytldr.api.article.view;

public final class ArticleViewExceptions {
  private ArticleViewExceptions() {}

  public static class Unavailable extends RuntimeException {
    public Unavailable() {
      super("article view service unavailable");
    }

    public Unavailable(Throwable cause) {
      super("article view service unavailable", cause);
    }
  }
}
