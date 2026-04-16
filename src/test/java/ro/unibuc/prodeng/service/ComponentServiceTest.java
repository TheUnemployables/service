package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.ComponentEntity;
import ro.unibuc.prodeng.repository.ComponentRepository;
import ro.unibuc.prodeng.request.CreateComponentRequest;
import ro.unibuc.prodeng.request.EditComponentRequest;
import ro.unibuc.prodeng.response.ComponentResponse;



import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComponentServiceTest {

    @Mock
    private ComponentRepository componentRepository;

    @InjectMocks
    private ComponentService componentService; 

    @Mock
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;
    //Test allfilters
    @Test
    void testGetComponents_withAllFilters_returnsPaginatedList() {
        //Arrange
        ComponentEntity entity = new ComponentEntity();
        entity.setId("id1");
        entity.setName("Arduino");
        entity.setDescription("Desc");
        entity.setCategory("Cat");
        entity.setQuantity(5);
        entity.setAvailableQuantity(5);
        entity.setIsConsumable(false);
        entity.setPhotoUrls(List.of());
        entity.setTags(List.of());
        entity.setInfoMarkdown("");
        entity.setCreatedAt(java.time.Instant.now());
        entity.setActive(true);

        List<ComponentEntity> mockEntities = List.of(entity);
        //How to answer
        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(ComponentEntity.class)))
                .thenReturn(mockEntities);
                
        when(mongoTemplate.count(any(org.springframework.data.mongodb.core.query.Query.class), eq(ComponentEntity.class)))
                .thenReturn(1L);
        //Apelare metoda
        Page<ComponentResponse> result = componentService.getComponents(
                "Cat",      
                false,      
                true,      
                "Ardu",   
                0,          
                10          
        );
        //Assert 
        assertEquals(1, result.getTotalElements());
        assertEquals("Arduino", result.getContent().get(0).name());
        //Verificare
        verify(mongoTemplate, times(1)).find(any(), eq(ComponentEntity.class));
        verify(mongoTemplate, times(1)).count(any(), eq(ComponentEntity.class));
    }

   @Test
    void testGetComponents_withNoFilters_returnsPaginatedList() {
        //Arrange
        ComponentEntity entity = new ComponentEntity();
        entity.setId("id2");
        entity.setName("Cablu");
        entity.setDescription("Descriere cablu");
        entity.setCategory("Electronice");
        
        entity.setQuantity(10);             
        entity.setAvailableQuantity(10);    
        entity.setIsConsumable(true);       
        
        entity.setPhotoUrls(List.of());
        entity.setTags(List.of());
        entity.setInfoMarkdown("");
        entity.setCreatedAt(java.time.Instant.now());
        entity.setActive(true);

        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(ComponentEntity.class)))
                .thenReturn(List.of(entity));
                
        when(mongoTemplate.count(any(org.springframework.data.mongodb.core.query.Query.class), eq(ComponentEntity.class)))
                .thenReturn(1L);
        //Act
        Page<ComponentResponse> result = componentService.getComponents(
                null, null, null, null, 0, 10
        );
        //Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Cablu", result.getContent().get(0).name());
        
        verify(mongoTemplate, times(1)).find(any(), eq(ComponentEntity.class));
    }
   @Test
    void testCreateComponent_validRequest_createsSuccessfully() {
        //Arrange
        CreateComponentRequest request = new CreateComponentRequest("Arduino", "Desc", "Cat", List.of(), 10, false, List.of(), "");
        
        ComponentEntity savedEntity = new ComponentEntity();
        savedEntity.setId("mock-id");
        savedEntity.setName("Arduino");
        savedEntity.setDescription("Desc");
        savedEntity.setCategory("Cat");
        savedEntity.setQuantity(10);
        savedEntity.setAvailableQuantity(10);
        savedEntity.setIsConsumable(false);
        savedEntity.setPhotoUrls(List.of());
        savedEntity.setTags(List.of());
        savedEntity.setInfoMarkdown("");
        savedEntity.setCreatedAt(java.time.Instant.now());
        savedEntity.setActive(true);

        when(componentRepository.save(any(ComponentEntity.class))).thenReturn(savedEntity);
        //act
        ComponentResponse response = componentService.createComponent(request);
        //Assert
        assertNotNull(response);
        assertEquals(10, response.availableQuantity());
        //Verify
        verify(componentRepository, times(1)).save(any(ComponentEntity.class));
    }

    @Test
    void testGetComponentById_whenExists_returnsComponent() throws EntityNotFoundException {
        ComponentEntity entity = new ComponentEntity();
        entity.setId("id1");
        entity.setName("Test");
        entity.setDescription("Desc");
        entity.setCategory("Cat");
        entity.setQuantity(5);
        entity.setAvailableQuantity(5);
        entity.setIsConsumable(false);
        entity.setPhotoUrls(List.of());
        entity.setTags(List.of());
        entity.setInfoMarkdown("");
        entity.setCreatedAt(java.time.Instant.now());
        entity.setActive(true);

        when(componentRepository.findById("id1")).thenReturn(Optional.of(entity));

        ComponentResponse response = componentService.getComponentById("id1");

        assertEquals("Test", response.name());
    }

    
    @Test
    void testGetComponentById_whenNotExists_throwsException() {
        when(componentRepository.findById("fake-id")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> componentService.getComponentById("fake-id"));
    }

    @Test
    void testGetComponents_withBlankFilters_skipsCriteria() {
        ComponentEntity entity = new ComponentEntity();
        entity.setId("id3");
        entity.setName("Rezistor");
        entity.setDescription("Descriere");
        entity.setCategory("Componente");
        entity.setQuantity(20);             
        entity.setAvailableQuantity(20);    
        entity.setIsConsumable(true);       
        entity.setPhotoUrls(List.of());
        entity.setTags(List.of());
        entity.setInfoMarkdown("");
        entity.setCreatedAt(java.time.Instant.now());
        entity.setActive(true);

        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(ComponentEntity.class)))
                .thenReturn(List.of(entity));
                
        when(mongoTemplate.count(any(org.springframework.data.mongodb.core.query.Query.class), eq(ComponentEntity.class)))
                .thenReturn(1L);

        Page<ComponentResponse> result = componentService.getComponents(
                "   ",      
                null,       
                false,      
                "",        
                0, 
                10
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Rezistor", result.getContent().get(0).name());
        
        verify(mongoTemplate, times(1)).find(any(), eq(ComponentEntity.class));
    }
    @Test
    void testUpdateComponent_changeConsumable_entersIfBranch() throws EntityNotFoundException {
        ComponentEntity existing = new ComponentEntity();
        existing.setId("id99");
        existing.setName("Motor");
        existing.setDescription("Desc");
        existing.setCategory("Mecanice");
        existing.setQuantity(5);
        existing.setAvailableQuantity(5);
        existing.setIsConsumable(false); 
        existing.setPhotoUrls(List.of());
        existing.setTags(List.of());
        existing.setInfoMarkdown("");
        existing.setCreatedAt(java.time.Instant.now());
        existing.setActive(true);

        EditComponentRequest request = new EditComponentRequest(
                "Motor", "Desc", "Mecanice", List.of(), 
                true,  
                List.of(), ""
        );

        when(componentRepository.findById("id99")).thenReturn(Optional.of(existing));
        when(componentRepository.save(any())).thenReturn(existing);

        ComponentResponse response = componentService.updateComponent("id99", request);

        assertTrue(response.isConsumable()); 
        verify(componentRepository, times(1)).save(existing);
    }
   @Test
    void testUpdateComponent_validData_updatesSuccessfully() throws EntityNotFoundException {
        ComponentEntity existing = new ComponentEntity();
        existing.setId("id1");
        existing.setName("Old Name");
        existing.setDescription("Desc");
        existing.setCategory("Cat");
        existing.setQuantity(5);
        existing.setAvailableQuantity(5);
        existing.setIsConsumable(false);
        existing.setPhotoUrls(List.of());
        existing.setTags(List.of());
        existing.setInfoMarkdown("");
        existing.setCreatedAt(java.time.Instant.now());
        existing.setActive(true);

        EditComponentRequest request = new EditComponentRequest("New Name", "Desc", "Cat", List.of(), false, List.of(), "");

        when(componentRepository.findById("id1")).thenReturn(Optional.of(existing));
        when(componentRepository.save(any())).thenReturn(existing);

        ComponentResponse response = componentService.updateComponent("id1", request);

        assertEquals("New Name", existing.getName());
        verify(componentRepository, times(1)).save(existing);
    }
   

    @Test
    void testDeleteComponent_noActiveLoans_softDeletesSuccessfully() throws EntityNotFoundException {
        ComponentEntity existing = new ComponentEntity();
        existing.setId("id1");
        existing.setActive(true);

        when(componentRepository.findById("id1")).thenReturn(Optional.of(existing));
        when(componentRepository.save(any())).thenReturn(existing);

        componentService.deleteComponent("id1");

        assertFalse(existing.getActive());
        verify(componentRepository, times(1)).save(existing);
    }

    @Test
    void testDeleteComponent_whenNotExists_throwsException() {
        when(componentRepository.findById("fake-id")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> componentService.deleteComponent("fake-id"));
        verify(componentRepository, never()).save(any());
    }
}