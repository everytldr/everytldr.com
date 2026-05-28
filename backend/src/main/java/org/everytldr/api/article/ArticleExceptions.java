package org.everytldr.api.article;

public final class ArticleExceptions {
  private ArticleExceptions() {}

  public static class NotFound extends RuntimeException {
    public NotFound(Long articleId) {
      super("article not found: " + articleId);
    }
  }
}
