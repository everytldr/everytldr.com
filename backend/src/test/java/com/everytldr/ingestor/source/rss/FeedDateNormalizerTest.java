package com.everytldr.ingestor.source.rss;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FeedDateNormalizerTest {

  @Test
  void addsColonToIsoOffset() {
    assertThat(FeedDateNormalizer.normalize("<pubDate>2026-05-08T08:25:43+0000</pubDate>"))
        .isEqualTo("<pubDate>2026-05-08T08:25:43+00:00</pubDate>");
  }

  @Test
  void removesZoneNameRepeatedBeforeOffset() {
    assertThat(
            FeedDateNormalizer.normalize("<pubDate>Fri, 08 May 2026 08:25:43 GMT+0000</pubDate>"))
        .isEqualTo("<pubDate>Fri, 08 May 2026 08:25:43 +0000</pubDate>");
  }

  @Test
  void keepsValidRfc822DateUnchanged() {
    String xml = "<pubDate>Fri, 08 May 2026 08:25:43 +0000</pubDate>";

    assertThat(FeedDateNormalizer.normalize(xml)).isEqualTo(xml);
  }

  @Test
  void keepsValidRfc822ZoneNameUnchanged() {
    String xml = "<pubDate>Fri, 08 May 2026 08:25:43 GMT</pubDate>";

    assertThat(FeedDateNormalizer.normalize(xml)).isEqualTo(xml);
  }

  @Test
  void keepsValidIsoDateUnchanged() {
    String xml = "<published>2026-05-08T08:25:43Z</published>";

    assertThat(FeedDateNormalizer.normalize(xml)).isEqualTo(xml);
  }

  @Test
  void normalizesNamespacePrefixedDateElement() {
    assertThat(FeedDateNormalizer.normalize("<dc:date>2026-05-08T08:25:43+0000</dc:date>"))
        .isEqualTo("<dc:date>2026-05-08T08:25:43+00:00</dc:date>");
  }

  @Test
  void keepsContentOutsideDateElementsUnchanged() {
    String xml =
        "<description>Published at 2026-05-08T08:25:43+0000 GMT+0900</description>"
            + "<pubDate>2026-05-08T08:25:43+0000</pubDate>";

    assertThat(FeedDateNormalizer.normalize(xml))
        .isEqualTo(
            "<description>Published at 2026-05-08T08:25:43+0000 GMT+0900</description>"
                + "<pubDate>2026-05-08T08:25:43+00:00</pubDate>");
  }

  @Test
  void keepsBlankInputUnchanged() {
    assertThat(FeedDateNormalizer.normalize("")).isEmpty();
    assertThat(FeedDateNormalizer.normalize(null)).isNull();
  }
}
