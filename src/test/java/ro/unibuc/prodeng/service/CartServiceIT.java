package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.CartEntity;
import ro.unibuc.prodeng.model.ComponentEntity;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.repository.ComponentRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.AddToCartRequest;
import ro.unibuc.prodeng.response.CartResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest
public class CartServiceIT {

    @Autowired
    private CartService cartService;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService; 

    @MockitoBean
    private TodoService todoService;

    private final String userId = "user123";
    private final String componentId = "comp123";

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(CartEntity.class);
        mongoTemplate.dropCollection(ComponentEntity.class);

        UserEntity mockUser = Mockito.mock(UserEntity.class);
        Mockito.when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));

        ComponentEntity component = new ComponentEntity();
        component.setId(componentId);
        component.setName("Placă Senzor IT");
        component.setQuantity(20);
        component.setAvailableQuantity(20);
        component.setIsConsumable(true);
        componentRepository.save(component);
    }

    @Test
    void testAddToCart_FullFlow() throws EntityNotFoundException {
        AddToCartRequest request = new AddToCartRequest(componentId, 5);
        CartResponse response = cartService.addToCart(userId, request);
        assertEquals(5, response.items().get(0).quantity());

        response = cartService.addToCart(userId, new AddToCartRequest(componentId, 2));
        assertEquals(7, response.items().get(0).quantity());
    }

    @Test
    void testAddToCart_InsufficientStock() {
        // Avem 20 în stoc, cerem 100 -> trebuie să dea eroare
        AddToCartRequest request = new AddToCartRequest(componentId, 100);
        assertThrows(IllegalArgumentException.class, () -> cartService.addToCart(userId, request));
    }

    @Test
    void testRemoveFromCart() throws EntityNotFoundException {
        cartService.addToCart(userId, new AddToCartRequest(componentId, 2));
        CartResponse response = cartService.removeFromCart(userId, componentId);
        assertTrue(response.items().isEmpty());
    }

    @Test
    void testSubmitCart_SuccessAndEmptyError() throws EntityNotFoundException {
        assertThrows(EntityNotFoundException.class, () -> cartService.submitCart("nonExistentUser"));

        cartService.addToCart(userId, new AddToCartRequest(componentId, 5));
        
        CartResponse submitResponse = cartService.submitCart(userId);
        
        assertEquals("SUBMITTED", submitResponse.status());

        ComponentEntity updated = componentRepository.findById(componentId).orElseThrow();
        assertEquals(15, updated.getAvailableQuantity());
    }
}