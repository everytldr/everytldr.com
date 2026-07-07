package com.everytldr.ingestor.ingestion;

public final class IngestionExceptions {
  private IngestionExceptions() {}

  public static class Retryable extends RuntimeException {
    public Retryable(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class Skippable extends RuntimeException {
    public Skippable(String message, Throwable cause) {
      super(message, cause);
    }
  }

  public static class Fatal extends RuntimeException {
    public Fatal(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
