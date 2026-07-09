package com.everytldr.api.article;

public final class ArticleCommentExceptions {
  private ArticleCommentExceptions() {}

  public static class InvalidParent extends RuntimeException {
    public InvalidParent(Long parentId) {
      super("invalid comment parent: " + parentId);
    }
  }

  public static class NotFound extends RuntimeException {
    public NotFound(Long commentId) {
      super("comment not found: " + commentId);
    }
  }

  public static class PasswordMismatch extends RuntimeException {
    public PasswordMismatch(Long commentId) {
      super("comment password mismatch: " + commentId);
    }
  }
}
