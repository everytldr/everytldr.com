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
  void listReturnsSeededCategoriesWithHierarchicalSlugs() throws Exception {
    mockMvc
        .perform(get("/api/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(146))
        .andExpect(jsonPath("$[0].slug").value("world"))
        .andExpect(jsonPath("$[1].slug").value("world-geopolitics"))
        .andExpect(jsonPath("$[2].slug").value("world-geopolitics-diplomacy"))
        .andExpect(jsonPath("$[11].slug").value("world-humanitarian-refugees"))
        .andExpect(jsonPath("$[12].slug").value("world-humanitarian-aid"))
        .andExpect(jsonPath("$[13].slug").value("politics"))
        .andExpect(jsonPath("$[25].slug").value("rights"))
        .andExpect(jsonPath("$[26].slug").value("rights-human_rights"))
        .andExpect(jsonPath("$[32].slug").value("rights-censorship"))
        .andExpect(jsonPath("$[44].slug").value("media"))
        .andExpect(jsonPath("$[45].slug").value("media-journalism"))
        .andExpect(jsonPath("$[65].slug").value("environment"))
        .andExpect(jsonPath("$[66].slug").value("environment-climate"))
        .andExpect(jsonPath("$[77].slug").value("environment-water"))
        .andExpect(jsonPath("$[78].slug").value("technology"))
        .andExpect(jsonPath("$[83].slug").value("technology-internet"))
        .andExpect(jsonPath("$[86].slug").value("science"))
        .andExpect(jsonPath("$[104].slug").value("culture"))
        .andExpect(jsonPath("$[111].slug").value("culture-arts-photography"))
        .andExpect(jsonPath("$[123].slug").value("sport-football-epl-arsenal"))
        .andExpect(jsonPath("$[145].slug").value("sport-global"));
  }

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
