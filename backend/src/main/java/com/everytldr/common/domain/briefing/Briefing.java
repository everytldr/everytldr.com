package com.everytldr.common.domain.briefing;

import com.everytldr.common.domain.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_briefing_date_language",
            columnNames = {"briefing_date", "language"}))
public class Briefing extends BaseEntity {
  @Column(nullable = false)
  private LocalDate briefingDate;

  @Column(nullable = false, length = 10)
  private String language;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(columnDefinition = "TEXT NOT NULL")
  private String content;

  private Briefing(LocalDate briefingDate, String language, String title, String content) {
    this.briefingDate = briefingDate;
    this.language = language;
    this.title = title;
    this.content = content;
  }

  public static Briefing create(
      LocalDate briefingDate, String language, String title, String content) {
    return new Briefing(briefingDate, language, title, content);
  }

  public void rewrite(String title, String content) {
    this.title = title;
    this.content = content;
  }

  public String extractExcerpt(int maxLength) {
    String lead = content.strip().split("\\R\\s*\\R", 2)[0].strip();
    if (lead.length() <= maxLength) {
      return lead;
    }
    return lead.substring(0, maxLength).stripTrailing() + "…";
  }
}
