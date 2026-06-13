package com.everytldr.api.category;

import com.everytldr.common.domain.category.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

public record CategoryListItem(@Schema(requiredMode = RequiredMode.REQUIRED) String slug) {
  static CategoryListItem from(Category category) {
    return new CategoryListItem(category.getSlug());
  }
}
