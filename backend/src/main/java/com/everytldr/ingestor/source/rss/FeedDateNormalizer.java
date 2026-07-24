package com.everytldr.ingestor.source.rss;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FeedDateNormalizer {

  private static final Pattern DATE_ELEMENT_PATTERN =
      Pattern.compile(
          "(<(?:[A-Za-z0-9_.-]+:)?(?:pubDate|date|published|updated|lastBuildDate)(?:\\s[^>]*)?>)([^<]+)(</)",
          Pattern.CASE_INSENSITIVE);
  private static final Pattern ZONE_PREFIXED_OFFSET_PATTERN =
      Pattern.compile("(?:GMT|UTC)\\s*(?=[+-]\\d{2})", Pattern.CASE_INSENSITIVE);
  private static final Pattern COLONLESS_ISO_OFFSET_PATTERN =
      Pattern.compile(
          "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?)([+-]\\d{2})(\\d{2})$");

  private FeedDateNormalizer() {}

  static String normalize(String xml) {
    if (xml == null || xml.isBlank()) {
      return xml;
    }

    Matcher matcher = DATE_ELEMENT_PATTERN.matcher(xml);
    StringBuilder normalized = new StringBuilder();
    while (matcher.find()) {
      String normalizedText = normalizeDateText(matcher.group(2));
      matcher.appendReplacement(
          normalized,
          Matcher.quoteReplacement(matcher.group(1) + normalizedText + matcher.group(3)));
    }
    matcher.appendTail(normalized);
    return normalized.toString();
  }

  private static String normalizeDateText(String dateText) {
    String trimmed = dateText.trim();
    if (trimmed.isEmpty()) {
      return dateText;
    }

    String withoutZonePrefix = ZONE_PREFIXED_OFFSET_PATTERN.matcher(trimmed).replaceAll("");
    String withIsoOffset =
        COLONLESS_ISO_OFFSET_PATTERN.matcher(withoutZonePrefix).replaceAll("$1$2:$3");
    return withIsoOffset.equals(trimmed) ? dateText : withIsoOffset;
  }
}
