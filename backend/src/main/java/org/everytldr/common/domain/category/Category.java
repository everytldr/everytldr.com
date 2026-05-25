package org.everytldr.common.domain.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.everytldr.common.domain.support.BaseEntity;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_category_slug", columnNames = "slug"))
public class Category extends BaseEntity {
  @Column(nullable = false, length = 50)
  private String slug;

  @Column(columnDefinition = "INT NOT NULL DEFAULT 0")
  private int sortOrder;

  private Category(String slug, int sortOrder) {
    this.slug = slug;
    this.sortOrder = sortOrder;
  }

  public static Category create(String slug, int sortOrder) {
    return new Category(slug, sortOrder);
  }
}
