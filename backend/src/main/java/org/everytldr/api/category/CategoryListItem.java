package org.everytldr.api.category;

import org.everytldr.common.domain.category.Category;

public record CategoryListItem(String slug, int sortOrder) {
  static CategoryListItem from(Category category) {
    return new CategoryListItem(category.getSlug(), category.getSortOrder());
  }
}
