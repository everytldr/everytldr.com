package com.everytldr.api.article;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class ArticleListCursor {
  private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

  private ArticleListCursor() {}

  record Decoded(Instant publishedAt, long id) {}

  static String encode(Instant publishedAt, long id) {
    String raw = publishedAt.toEpochMilli() + ":" + id;
    return ENCODER.encodeToString(raw.getBytes(StandardCharsets.UTF_8));
  }

  static Decoded decode(String token) {
    try {
      String raw = new String(DECODER.decode(token.trim()), StandardCharsets.UTF_8);
      int separatorIdx = raw.indexOf(':');
      if (separatorIdx < 0) {
        throw new IllegalArgumentException("missing separator");
      }
      long millis = Long.parseLong(raw.substring(0, separatorIdx));
      long id = Long.parseLong(raw.substring(separatorIdx + 1));
      return new Decoded(Instant.ofEpochMilli(millis), id);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid cursor", e);
    }
  }
}
