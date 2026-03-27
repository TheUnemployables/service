package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.repository.ComponentRepository;
import ro.unibuc.prodeng.request.CreateComponentRequest;
import ro.unibuc.prodeng.request.EditComponentRequest;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ComponentController Integration Tests")
class ComponentControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private ObjectMapper objectMapper;


    @BeforeEach
    void cleanUp() {
        componentRepository.deleteAll();
    }

    private String createComponent(String name, String category, int quantity, boolean isConsumable) throws Exception {
        CreateComponentRequest request = new CreateComponentRequest(
                name, "Test desc", category, List.of(), quantity, isConsumable, List.of(), "Test info"
        );

        String response = mockMvc.perform(post("/api/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.availableQuantity").value(quantity))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void testCreateAndGetComponent_validCreation_retrievesSuccessfully() throws Exception {
        String componentId = createComponent("Arduino Uno", "Electronice", 15, false);

        mockMvc.perform(get("/api/components/" + componentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arduino Uno"))
                .andExpect(jsonPath("$.category").value("Electronice"))
                .andExpect(jsonPath("$.availableQuantity").value(15));
        boolean existsInDb = componentRepository.findById(componentId).isPresent();
        org.junit.jupiter.api.Assertions.assertTrue(existsInDb, "Componenta trebuie să existe fizic in baza de date!");
    }
    @Test
    void testGetComponents_multipleComponents_filtersCorrectly() throws Exception {
        createComponent("Cablu USB", "Cabluri", 50, true);
        createComponent("Cablu HDMI", "Cabluri", 20, true);
        createComponent("Senzor miscare", "Senzori", 10, false);

        mockMvc.perform(get("/api/components").param("category", "Cabluri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/components").param("search", "Senzor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testUpdateComponent_validUpdate_updatesSuccessfully() throws Exception {
        String componentId = createComponent("Arduino Uno", "Electronice", 15, false);

        EditComponentRequest updateRequest = new EditComponentRequest(
                "Arduino Uno R3", "Descriere noua", "Microcontrollere", List.of(), false, List.of(), "Info nou"
        );

        mockMvc.perform(put("/api/components/" + componentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arduino Uno R3"))
                .andExpect(jsonPath("$.category").value("Microcontrollere"));
    }

    @Test
    void testDeleteComponent_existingComponent_softDeletesSuccessfully() throws Exception {
        String componentId = createComponent("Multimetru", "Aparate", 5, false);

        mockMvc.perform(delete("/api/components/" + componentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void testGetComponentById_nonExistentComponent_returnsNotFound() throws Exception {
        String nonExistentId = "fake-id-123";

        mockMvc.perform(get("/api/components/" + nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateComponent_invalidRequestMissingName_returnsBadRequest() throws Exception {
        CreateComponentRequest invalidRequest = new CreateComponentRequest(
                null, "Desc", "Cat", List.of(), 10, false, List.of(), ""
        );

        mockMvc.perform(post("/api/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}