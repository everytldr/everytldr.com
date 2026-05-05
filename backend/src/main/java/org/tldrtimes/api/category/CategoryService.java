package org.tldrtimes.api.category;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tldrtimes.common.domain.category.Category;
import org.tldrtimes.common.domain.category.CategoryRepository;

@Service
@Profile("api")
public class CategoryService {
  private final CategoryRepository categoryRepository;

  public CategoryService(CategoryRepository categoryRepository) {
    this.categoryRepository = categoryRepository;
  }

  @Transactional(readOnly = true)
  public List<Category> listAll() {
    return categoryRepository.findAllByOrderBySortOrderAscIdAsc();
  }
}
