package ro.unibuc.prodeng.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.model.ComponentEntity;
import ro.unibuc.prodeng.model.UserEntity;
import ro.unibuc.prodeng.repository.ComponentRepository;
import ro.unibuc.prodeng.repository.CartRepository;
import ro.unibuc.prodeng.repository.UserRepository;
import ro.unibuc.prodeng.request.AddToCartRequest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@DisplayName("Cart Integration Tests (Fixed Records)")
class CartServiceIT extends IntegrationTestBase {

   @Autowired
   private MockMvc mockMvc;

   @Autowired
   private ComponentRepository componentRepository;

   @Autowired
   private CartRepository cartRepository;

   @Autowired
   private UserRepository userRepository;

   @Autowired
   private ObjectMapper objectMapper;

   private final String userId = "user123";
   private final String componentId = "comp123";

   @BeforeEach
   void setup() {
      cartRepository.deleteAll();
      componentRepository.deleteAll();
      userRepository.deleteAll();

      // Corecție pentru Record: pasam datele direct în constructor
      UserEntity user = new UserEntity(userId, "Profesor Demo", "prof@unibuc.ro");
      userRepository.save(user);

      // Creăm o componentă cu stoc disponibil
      ComponentEntity component = new ComponentEntity();
      component.setId(componentId);
      component.setName("Piesa Test");
      component.setAvailableQuantity(10);
      componentRepository.save(component);
   }

   @Test
   void testAddToCart_andSubmit_FullIntegrationFlow() throws Exception {
      AddToCartRequest request = new AddToCartRequest(componentId, 2);
      
      mockMvc.perform(post("/api/carts/" + userId + "/add")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
              .andExpect(jsonPath("$.items[0].quantity", is(2)));

      mockMvc.perform(get("/api/carts/" + userId))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.userID", is(userId)));

      mockMvc.perform(post("/api/carts/" + userId + "/submit"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.status", is("SUBMITTED")));
   }
}