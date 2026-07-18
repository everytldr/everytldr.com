package com.everytldr.api.support.visitor;

import java.util.Objects;

public record AnonymousVisitor(String visitorHash) {
  public AnonymousVisitor {
    Objects.requireNonNull(visitorHash, "visitorHash must not be null");
    if (visitorHash.isBlank()) {
      throw new IllegalArgumentException("visitorHash must not be blank");
    }
  }
}
