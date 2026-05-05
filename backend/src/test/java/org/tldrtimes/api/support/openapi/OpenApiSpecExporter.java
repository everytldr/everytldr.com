package org.tldrtimes.api.support.openapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.tldrtimes.TestcontainersConfig;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
@ActiveProfiles({"api", "test"})
@EnabledIfSystemProperty(named = "openapi.export", matches = "true")
class OpenApiSpecExporter {
  @Autowired private MockMvc mockMvc;

  @Test
  void writeSpec() throws Exception {
    String body =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    String outputPath = System.getProperty("openapi.output", "../docs/openapi.json");
    Path target = Paths.get(outputPath).toAbsolutePath().normalize();
    Files.createDirectories(target.getParent());

    ObjectMapper mapper =
        new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    Object tree = mapper.readValue(body, Object.class);
    Files.writeString(
        target, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree) + "\n");
  }
}
