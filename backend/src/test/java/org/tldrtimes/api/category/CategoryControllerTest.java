package org.tldrtimes.api.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.tldrtimes.TestcontainersConfig;
import org.tldrtimes.common.domain.category.Category;
import org.tldrtimes.common.domain.category.CategoryRepository;

@SpringBootTest
@Import(TestcontainersConfig.class)
@ActiveProfiles("test")
@Transactional
class CategoryControllerTest {
  @Autowired
  private CategoryRepository categoryRepository;

  @Test
  void ordersBySortOrderThenIdAscending() {
    Category tech = categoryRepository.saveAndFlush(Category.create("tech", 2));
    Category football = categoryRepository.saveAndFlush(Category.create("football", 0));
    Category tieA = categoryRepository.saveAndFlush(Category.create("tie-a", 1));
    Category tieB = categoryRepository.saveAndFlush(Category.create("tie-b", 1));

    List<Category> rows = categoryRepository.findAllByOrderBySortOrderAscIdAsc();

    assertThat(rows)
        .extracting(Category::getId)
        .containsExactly(football.getId(), tieA.getId(), tieB.getId(), tech.getId());
  }
}
