package org.tldrtimes.api.category;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
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
  @Autowired private CategoryRepository categoryRepository;

  @Test
  void ordersBySortOrderThenIdAscending() {
    Category football = categoryRepository.saveAndFlush(Category.create("category-test-football", 0));
    Category tieA = categoryRepository.saveAndFlush(Category.create("category-test-tie-a", 1));
    Category tieB = categoryRepository.saveAndFlush(Category.create("category-test-tie-b", 1));
    Category tech = categoryRepository.saveAndFlush(Category.create("category-test-tech", 2));

    Set<String> targetSlugs =
        Set.of(
            "category-test-football",
            "category-test-tie-a",
            "category-test-tie-b",
            "category-test-tech");
    List<Category> rows =
        categoryRepository.findAllByOrderBySortOrderAscIdAsc().stream()
            .filter(category -> targetSlugs.contains(category.getSlug()))
            .toList();

    assertThat(rows)
        .extracting(Category::getId)
        .containsExactly(football.getId(), tieA.getId(), tieB.getId(), tech.getId());
  }
}
