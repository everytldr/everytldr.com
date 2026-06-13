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
  void listReturnsSeededCategoriesWithMergedHierarchicalSlugs() throws Exception {
    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(146))
        .andExpect(jsonPath("$[0].slug").value("culture"))
        .andExpect(jsonPath("$[145].slug").value("world-humanitarian-refugees"))
        .andExpect(jsonPath("$[?(@.slug == 'society-rights')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'society-media')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'society-education')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'technology-science')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'rights')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'media')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'education')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'science')]").isEmpty());
  }

  @Test
  void listReturnsCategoriesOrderedBySlug() throws Exception {
    categoryRepository.saveAndFlush(Category.create("zzz-test-b"));
    categoryRepository.saveAndFlush(Category.create("zzz-test-a"));

    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[146].slug").value("zzz-test-a"))
        .andExpect(jsonPath("$[147].slug").value("zzz-test-b"));
  }
}
