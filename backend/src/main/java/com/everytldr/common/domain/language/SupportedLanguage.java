package com.everytldr.common.domain.language;

import java.util.Locale;
import java.util.Optional;

public enum SupportedLanguage {
  KOREAN("ko"),
  ENGLISH("en");

  public static final SupportedLanguage DEFAULT = ENGLISH;

  private final String code;

  SupportedLanguage(String code) {
    this.code = code;
  }

  public String code() {
    return code;
  }

  public Locale toLocale() {
    return Locale.forLanguageTag(code);
  }

  public static Optional<SupportedLanguage> fromCode(String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }

    String primary = code.toLowerCase(Locale.ROOT).split("[-_]", 2)[0];
    for (SupportedLanguage language : values()) {
      if (language.code.equals(primary)) {
        return Optional.of(language);
      }
    }

    return Optional.empty();
  }

  public static Optional<SupportedLanguage> fromLocale(Locale locale) {
    if (locale == null) {
      return Optional.empty();
    }

    return fromCode(locale.getLanguage());
  }
}
