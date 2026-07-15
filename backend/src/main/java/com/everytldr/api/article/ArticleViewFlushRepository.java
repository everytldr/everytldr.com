package com.everytldr.api.article;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Profile("api")
public class ArticleViewFlushRepository {
  private static final int BATCH_SIZE = 100;

  private final JdbcTemplate jdbcTemplate;

  public boolean registerBatch(String batchId) {
    return jdbcTemplate.update(
            "INSERT IGNORE INTO article_view_flush_history (batch_id) VALUES (?)", batchId)
        == 1;
  }

  public void incrementViewCounts(Map<Long, Long> deltas) {
    jdbcTemplate.batchUpdate(
        "UPDATE article SET view_count = view_count + ? WHERE id = ?",
        new ArrayList<>(deltas.entrySet()),
        BATCH_SIZE,
        (statement, entry) -> {
          statement.setLong(1, entry.getValue());
          statement.setLong(2, entry.getKey());
        });
  }

  public int deleteHistoryBeforeExcluding(
      Instant cutoff, Collection<String> protectedBatchIds, int batchSize) {
    Objects.requireNonNull(cutoff, "cutoff must not be null");
    Objects.requireNonNull(protectedBatchIds, "protectedBatchIds must not be null");

    if (protectedBatchIds.isEmpty()) {
      return jdbcTemplate.update(
          "DELETE FROM article_view_flush_history WHERE created_at < ? ORDER BY created_at LIMIT ?",
          Timestamp.from(cutoff),
          batchSize);
    }

    String placeholders =
        String.join(", ", protectedBatchIds.stream().map(batchId -> "?").toList());
    ArrayList<Object> parameters = new ArrayList<>();
    parameters.add(Timestamp.from(cutoff));
    parameters.addAll(protectedBatchIds);
    parameters.add(batchSize);
    return jdbcTemplate.update(
        "DELETE FROM article_view_flush_history "
            + "WHERE created_at < ? AND batch_id NOT IN ("
            + placeholders
            + ") ORDER BY created_at LIMIT ?",
        parameters.toArray());
  }
}
