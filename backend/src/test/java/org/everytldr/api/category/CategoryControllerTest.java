package org.everytldr.api.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.everytldr.TestcontainersConfig;
import org.everytldr.common.domain.category.Category;
import org.everytldr.common.domain.category.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles({"api", "test"})
@Transactional
class CategoryControllerTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private CategoryRepository categoryRepository;

  @Test
  void listReturnsCategoriesOrderedBySortOrderThenId() throws Exception {
    categoryRepository.saveAndFlush(Category.create("tech", 2));
    categoryRepository.saveAndFlush(Category.create("football", 0));
    categoryRepository.saveAndFlush(Category.create("tie-a", 1));
    categoryRepository.saveAndFlush(Category.create("tie-b", 1));

    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].slug").value("football"))
        .andExpect(jsonPath("$[1].slug").value("tie-a"))
        .andExpect(jsonPath("$[2].slug").value("tie-b"))
        .andExpect(jsonPath("$[3].slug").value("tech"));
  }
}
