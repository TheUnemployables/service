package ro.unibuc.prodeng.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ro.unibuc.prodeng.model.CartEntity;
import ro.unibuc.prodeng.model.ComponentEntity;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.service.UserService;
import ro.unibuc.prodeng.service.TodoService;
import ro.unibuc.prodeng.request.AddToCartRequest;
import ro.unibuc.prodeng.response.CartResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CartE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TodoService todoService;

    private final String userId = "userE2E";
    private final String componentId = "compE2E";

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(CartEntity.class);
        mongoTemplate.dropCollection(ComponentEntity.class);
        UserEntity mockUser = Mockito.mock(UserEntity.class);
        Mockito.when(userRepository.findById(anyString())).thenReturn(Optional.of(mockUser));

        ComponentEntity component = new ComponentEntity();
        component.setId(componentId);
        component.setName("Arduino Mega E2E");
        component.setQuantity(10);
        component.setAvailableQuantity(10);
        mongoTemplate.save(component);
    }

    @Test
    void testFullCycleIncludingDelete() {
        AddToCartRequest addRequest = new AddToCartRequest(componentId, 2);
        ResponseEntity<CartResponse> addRes = restTemplate.postForEntity("/api/carts/"+userId+"/add", addRequest, CartResponse.class);
        assertEquals(HttpStatus.OK, addRes.getStatusCode());

        ResponseEntity<CartResponse> getRes = restTemplate.getForEntity("/api/carts/"+userId, CartResponse.class);
        assertEquals(1, getRes.getBody().items().size());

        ResponseEntity<CartResponse> deleteRes = restTemplate.exchange(
                "/api/carts/" + userId + "/remove/" + componentId,
                HttpMethod.DELETE,
                null,
                CartResponse.class
        );
        assertEquals(HttpStatus.OK, deleteRes.getStatusCode());
        assertTrue(deleteRes.getBody().items().isEmpty());

        restTemplate.postForEntity("/api/carts/"+userId+"/add", addRequest, CartResponse.class);
        ResponseEntity<CartResponse> submitRes = restTemplate.postForEntity("/api/carts/"+userId+"/submit", null, CartResponse.class);
        assertEquals("SUBMITTED", submitRes.getBody().status());
    }
}