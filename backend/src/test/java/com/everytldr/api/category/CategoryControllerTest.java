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
  void listReturnsSeededLeafCategories() throws Exception {
    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(99))
        .andExpect(jsonPath("$[0].slug").value("culture-arts-books"))
        .andExpect(jsonPath("$[98].slug").value("world-humanitarian-disaster_response"))
        .andExpect(jsonPath("$[?(@.slug == 'sport-football-epl-aston_villa')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'sport-events')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'technology-internet_platforms')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'society-rights-free_speech_censorship')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'economy-consumer')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'health-wellness')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'society-activism')]").isNotEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'rights')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'media')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'education')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'science')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'sport-football-arsenal')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'technology-companies')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'environment-water')]").isEmpty())
        .andExpect(jsonPath("$[?(@.slug == 'society-profiles')]").isEmpty());
  }

  @Test
  void listReturnsCategoriesOrderedBySlug() throws Exception {
    categoryRepository.saveAndFlush(Category.create("zzz-test-b"));
    categoryRepository.saveAndFlush(Category.create("zzz-test-a"));

    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[99].slug").value("zzz-test-a"))
        .andExpect(jsonPath("$[100].slug").value("zzz-test-b"));
  }
}
