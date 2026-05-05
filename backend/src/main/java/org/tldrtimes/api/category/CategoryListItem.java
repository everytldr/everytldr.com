package org.tldrtimes.api.category;

import org.tldrtimes.common.domain.category.Category;

public record CategoryListItem(String slug, int sortOrder) {
  static CategoryListItem from(Category category) {
    return new CategoryListItem(category.getSlug(), category.getSortOrder());
  }
}
