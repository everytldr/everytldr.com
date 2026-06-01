package com.everytldr.api.article;

public final class ArticleCommentExceptions {
  private ArticleCommentExceptions() {}

  public static class InvalidParent extends RuntimeException {
    public InvalidParent(Long parentId) {
      super("invalid comment parent: " + parentId);
    }
  }
}
