package com.everytldr.api.category;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.everytldr.TestcontainersConfig;
import com.everytldr.common.domain.category.Category;
import com.everytldr.common.domain.category.CategoryRepository;
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
    categoryRepository.saveAndFlush(Category.create("test-tech", -1));
    categoryRepository.saveAndFlush(Category.create("test-football", -3));
    categoryRepository.saveAndFlush(Category.create("test-tie-a", -2));
    categoryRepository.saveAndFlush(Category.create("test-tie-b", -2));

    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].slug").value("test-football"))
        .andExpect(jsonPath("$[1].slug").value("test-tie-a"))
        .andExpect(jsonPath("$[2].slug").value("test-tie-b"))
        .andExpect(jsonPath("$[3].slug").value("test-tech"));
  }
}
