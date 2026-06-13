package com.everytldr.common.domain.category;

import com.everytldr.common.domain.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_category_slug", columnNames = "slug"))
public class Category extends BaseEntity {
  @Column(nullable = false, length = 50)
  private String slug;

  private Category(String slug) {
    this.slug = slug;
  }

  public static Category create(String slug) {
    return new Category(slug);
  }
}
