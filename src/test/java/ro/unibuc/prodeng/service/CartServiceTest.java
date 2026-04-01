package ro.unibuc.prodeng.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.CartEntity;
import ro.unibuc.prodeng.model.ComponentEntity;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.repository.CartRepository;
import ro.unibuc.prodeng.repository.ComponentRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.AddToCartRequest;
import ro.unibuc.prodeng.response.CartResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private ComponentRepository componentRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CartService cartService;

    @Test
    void getActiveCart_ReturnsExistingCart_WhenUserExists() throws EntityNotFoundException {
        // ARRANGE
        String userId = "user123";
        UserEntity mockUser = new UserEntity(userId, "Alex", "alex@test.com");
        CartEntity mockCart = new CartEntity();
        mockCart.setUserID(userId);
        mockCart.setStatus(CartEntity.CartStatus.OPEN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserIDAndStatus(userId, CartEntity.CartStatus.OPEN)).thenReturn(Optional.of(mockCart));

        // ACT
        CartResponse response = cartService.getActiveCart(userId);

        // ASSERT
        assertNotNull(response);
        assertEquals(userId, response.userID());
        assertEquals("OPEN", response.status());
    }

    @Test
    void addToCart_AddsNewItem_WhenStockIsSufficient() throws EntityNotFoundException {
        // ARRANGE
        String userId = "user123";
        String compId = "comp1";
        AddToCartRequest request = new AddToCartRequest(compId, 2);

        ComponentEntity mockComponent = new ComponentEntity();
        mockComponent.setId(compId);
        mockComponent.setName("Arduino");
        mockComponent.setAvailableQuantity(5);

        CartEntity mockCart = new CartEntity();
        mockCart.setUserID(userId);
        mockCart.setStatus(CartEntity.CartStatus.OPEN);

        when(componentRepository.findById(compId)).thenReturn(Optional.of(mockComponent));
        when(cartRepository.findByUserIDAndStatus(userId, CartEntity.CartStatus.OPEN)).thenReturn(Optional.of(mockCart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(mockCart);

        // ACT
        CartResponse response = cartService.addToCart(userId, request);

        // ASSERT
        assertNotNull(response);
        assertEquals(1, mockCart.getItems().size());
        assertEquals(compId, mockCart.getItems().get(0).getComponentId());
        assertEquals(2, mockCart.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }

    @Test
    void addToCart_ThrowsException_WhenStockIsNotSufficient() {
        // ARRANGE
        String userId = "user123";
        String compId = "comp1";
        AddToCartRequest request = new AddToCartRequest(compId, 10);

        ComponentEntity mockComponent = new ComponentEntity();
        mockComponent.setId(compId);
        mockComponent.setAvailableQuantity(5);

        when(componentRepository.findById(compId)).thenReturn(Optional.of(mockComponent));

        // ACT & ASSERT
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            cartService.addToCart(userId, request);
        });
        assertEquals("Not enough components in stock.", exception.getMessage());
    }

    @Test
    void submitCart_UpdatesStockAndChangesStatus_WhenCartIsValid() throws EntityNotFoundException {
        // ARRANGE
        String userId = "user123";
        CartEntity mockCart = new CartEntity();
        mockCart.setUserID(userId);
        mockCart.setStatus(CartEntity.CartStatus.OPEN);
        mockCart.getItems().add(new CartEntity.CartItem("comp1", 2));

        ComponentEntity mockComponent = new ComponentEntity();
        mockComponent.setId("comp1");
        mockComponent.setAvailableQuantity(5);

        when(cartRepository.findByUserIDAndStatus(userId, CartEntity.CartStatus.OPEN)).thenReturn(Optional.of(mockCart));
        when(componentRepository.findById("comp1")).thenReturn(Optional.of(mockComponent));
        when(componentRepository.save(any(ComponentEntity.class))).thenReturn(mockComponent);
        when(cartRepository.save(any(CartEntity.class))).thenReturn(mockCart);

        // ACT
        CartResponse response = cartService.submitCart(userId);

        // ASSERT
        assertNotNull(response);
        assertEquals("SUBMITTED", response.status());
        assertEquals(3, mockComponent.getAvailableQuantity());
        verify(componentRepository, times(1)).save(mockComponent);
        verify(cartRepository, times(1)).save(mockCart);
    }

    @Test
    void removeFromCart_RemovesExistingItem_WhenItemIsPresent() throws EntityNotFoundException {
        // ARRANGE
        String userId = "user123";
        String componentId = "comp1";

        CartEntity mockCart = new CartEntity();
        mockCart.setUserID(userId);
        mockCart.setStatus(CartEntity.CartStatus.OPEN);
        mockCart.getItems().add(new CartEntity.CartItem(componentId, 2));

        when(cartRepository.findByUserIDAndStatus(userId, CartEntity.CartStatus.OPEN)).thenReturn(Optional.of(mockCart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(mockCart);

        // ACT
        CartResponse response = cartService.removeFromCart(userId, componentId);

        // ASSERT
        assertNotNull(response);
        assertTrue(mockCart.getItems().isEmpty());
        verify(cartRepository, times(1)).save(mockCart);
    }

    @Test
    void removeFromCart_ThrowsEntityNotFound_WhenItemNotInCart() {
        // ARRANGE
        String userId = "user123";
        String componentId = "comp1";

        CartEntity mockCart = new CartEntity();
        mockCart.setUserID(userId);
        mockCart.setStatus(CartEntity.CartStatus.OPEN);

        when(cartRepository.findByUserIDAndStatus(userId, CartEntity.CartStatus.OPEN)).thenReturn(Optional.of(mockCart));

        // ACT & ASSERT
        assertThrows(EntityNotFoundException.class, () -> cartService.removeFromCart(userId, componentId));
    }

    @Test
    void getActiveCart_CreatesAndReturnsNewCart_WhenNoActiveCartExists() throws EntityNotFoundException {
        // ARRANGE
        String userId = "user123";
        UserEntity mockUser = new UserEntity(userId, "Alex", "alex@test.com");

        CartEntity createdCart = new CartEntity();
        createdCart.setId("newCartId");
        createdCart.setUserID(userId);
        createdCart.setStatus(CartEntity.CartStatus.OPEN);

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserIDAndStatus(userId, CartEntity.CartStatus.OPEN)).thenReturn(Optional.empty());
        when(cartRepository.save(any(CartEntity.class))).thenReturn(createdCart);

        // ACT
        CartResponse response = cartService.getActiveCart(userId);

        // ASSERT
        assertNotNull(response);
        assertEquals("newCartId", response.id());
        assertEquals("OPEN", response.status());
    }

    @Test
    void addToCart_IncrementsQuantity_WhenItemAlreadyExists() throws EntityNotFoundException {
        // ARRANGE
        String userId = "user123";
        String compId = "comp1";
        AddToCartRequest request = new AddToCartRequest(compId, 2);

        ComponentEntity mockComponent = new ComponentEntity();
        mockComponent.setId(compId);
        mockComponent.setAvailableQuantity(5);

        CartEntity mockCart = new CartEntity();
        mockCart.setUserID(userId);
        mockCart.setStatus(CartEntity.CartStatus.OPEN);
        mockCart.getItems().add(new CartEntity.CartItem(compId, 1));

        when(componentRepository.findById(compId)).thenReturn(Optional.of(mockComponent));
        when(cartRepository.findByUserIDAndStatus(userId, CartEntity.CartStatus.OPEN)).thenReturn(Optional.of(mockCart));
        when(cartRepository.save(any(CartEntity.class))).thenReturn(mockCart);

        // ACT
        CartResponse response = cartService.addToCart(userId, request);

        // ASSERT
        assertNotNull(response);
        assertEquals(3, mockCart.getItems().get(0).getQuantity());
        verify(cartRepository, times(1)).save(any(CartEntity.class));
    }
}