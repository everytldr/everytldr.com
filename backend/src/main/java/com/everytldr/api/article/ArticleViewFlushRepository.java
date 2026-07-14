package com.everytldr.api.article;

import java.util.ArrayList;
import java.util.Map;
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
}
