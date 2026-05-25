package org.everytldr.api.support.pagination;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Pagination {
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 50;

  private Pagination() {}

  public static int clampSize(Integer requested) {
    if (requested == null) {
      return DEFAULT_SIZE;
    }
    if (requested < 1 || requested > MAX_SIZE) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "size must be between 1 and %d".formatted(MAX_SIZE));
    }

    return requested;
  }

  public record Page<T>(List<T> items, T nextStart) {
    public static <T> Page<T> from(List<T> rows, int pageSize) {
      boolean hasMore = rows.size() > pageSize;
      List<T> items = hasMore ? rows.subList(0, pageSize) : rows;
      T nextStart = hasMore ? items.getLast() : null;
      return new Page<>(items, nextStart);
    }
  }
}
