package ro.unibuc.prodeng.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ro.unibuc.prodeng.request.CreateComponentRequest;
import ro.unibuc.prodeng.response.ComponentResponse;
import ro.unibuc.prodeng.service.ComponentService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class) 
class ComponentControllerTest {

    @Mock
    private ComponentService componentService;

    @InjectMocks
    private ComponentController componentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(componentController).build();
    }
    //Post test
    @Test
    void testCreateComponent_validRequestProvided_createsAndReturnsComponent() throws Exception {
        CreateComponentRequest request = new CreateComponentRequest(
                "Arduino", "Desc", "Cat", List.of(), 10, false, List.of(), ""
        );
        
        ComponentResponse expectedResponse = new ComponentResponse(
                "mock-id", "Arduino", "Desc", "Cat", List.of(), 10, 10, false, List.of(), "", java.time.Instant.now()
        );

        when(componentService.createComponent(any(CreateComponentRequest.class))).thenReturn(expectedResponse);
            //act and assert
        mockMvc.perform(post("/api/components")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("mock-id"))
                .andExpect(jsonPath("$.name").value("Arduino"))
                .andExpect(jsonPath("$.availableQuantity").value(10));

        verify(componentService, times(1)).createComponent(any(CreateComponentRequest.class));
    }
    @Test
    void testGetComponents_withFilters_returnsPaginatedList() throws Exception {
        when(componentService.getComponents(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(null);

        mockMvc.perform(get("/api/components")
                        .param("category", "Electronice")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()); 

        verify(componentService, times(1)).getComponents(any(), any(), any(), any(), anyInt(), anyInt());
    }
    @Test
    void testGetComponentById_existingId_returnsComponent() throws Exception {
        String componentId = "id-1";
        ComponentResponse expectedResponse = new ComponentResponse(
                componentId, "Arduino", "Desc", "Electronice", List.of(), 10, 10, false, List.of(), "", java.time.Instant.now()
        );

        when(componentService.getComponentById(componentId)).thenReturn(expectedResponse);

        mockMvc.perform(get("/api/components/{id}", componentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(componentId))
                .andExpect(jsonPath("$.name").value("Arduino"));

        verify(componentService, times(1)).getComponentById(componentId);
    }

    @Test
    void testUpdateComponent_validRequest_returnsUpdatedComponent() throws Exception {
        String componentId = "id-1";
        ro.unibuc.prodeng.request.EditComponentRequest updateRequest = new ro.unibuc.prodeng.request.EditComponentRequest(
                "Arduino R3", "Descriere Noua", "Microcontrollere", List.of(), false, List.of(), ""
        );

        ComponentResponse expectedResponse = new ComponentResponse(
                componentId, "Arduino R3", "Descriere Noua", "Microcontrollere", List.of(), 10, 10, false, List.of(), "", java.time.Instant.now()
        );

        when(componentService.updateComponent(eq(componentId), any(ro.unibuc.prodeng.request.EditComponentRequest.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(put("/api/components/{id}", componentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arduino R3"))
                .andExpect(jsonPath("$.category").value("Microcontrollere"));

        verify(componentService, times(1)).updateComponent(eq(componentId), any(ro.unibuc.prodeng.request.EditComponentRequest.class));
    }
    @Test
    void testDeleteComponent_existingId_returnsNoContent() throws Exception {
        String componentId = "id-1";
        doNothing().when(componentService).deleteComponent(componentId);


        mockMvc.perform(delete("/api/components/{id}", componentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(componentService, times(1)).deleteComponent(componentId);
    }
}