package ro.unibuc.prodeng.e2e.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ro.unibuc.prodeng.request.AddToCartRequest;
import ro.unibuc.prodeng.request.CreateUserRequest;
import ro.unibuc.prodeng.response.CartResponse;
import ro.unibuc.prodeng.response.UserResponse;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CartSteps {

   private static final String BASE_URL = "http://localhost:8080";

   private final RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
   private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

   private ResponseEntity<String> latestResponse;
   private String lastCreatedUserId;

   @Given("a user named {string} with email {word} exists")
   public void ensureUserExists(String name, String email) throws Exception {
      CreateUserRequest request = new CreateUserRequest(name, email);
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<CreateUserRequest> entity = new HttpEntity<>(request, headers);

      try {
         ResponseEntity<String> response = restTemplate.postForEntity(BASE_URL + "/api/users", entity, String.class);
         UserResponse user = objectMapper.readValue(response.getBody(), UserResponse.class);
         lastCreatedUserId = user.id();
      } catch (HttpClientErrorException.BadRequest e) {
         ResponseEntity<String> response = restTemplate.getForEntity(BASE_URL + "/api/users/by-email?email=" + email, String.class);
         UserResponse user = objectMapper.readValue(response.getBody(), UserResponse.class);
         lastCreatedUserId = user.id();
      }
   }

   @Given("a component named {string} with id {string} exists")
   public void ensureComponentExists(String name, String id) {
      Map<String, Object> componentRequest = new HashMap<>();
      componentRequest.put("id", id);
      componentRequest.put("name", name);
      componentRequest.put("description", "Piesa creata automat de testul E2E");
      componentRequest.put("category", "E2E-Tests");
      componentRequest.put("stock", 100);
      componentRequest.put("isConsumable", false);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, Object>> entity = new HttpEntity<>(componentRequest, headers);

      try {
         restTemplate.postForEntity(BASE_URL + "/api/components", entity, String.class);
      } catch (Exception e) {
         // Daca exista deja, e ok, mergem mai departe
         System.out.println("Info: Componenta probabil exista deja sau eroare minora: " + e.getMessage());
      }
   }

   @When("the client adds {int} units of {string} to the cart for {string}")
   public void addItem(int qty, String compId, String userId) {
      // Ajustare: daca userId din feature este "user123" sau contine numele profesorului, 
      // folosim ID-ul real creat anterior
      String targetId = (userId.equals("user123") || userId.contains("Profesor")) ? lastCreatedUserId : userId;
      
      AddToCartRequest request = new AddToCartRequest(compId, qty);
      latestResponse = restTemplate.postForEntity(BASE_URL + "/api/carts/" + targetId + "/add", request, String.class);
   }

   @Then("the cart response status code is {int}")
   public void verifyStatusCode(int statusCode) {
      assertThat(latestResponse.getStatusCode().value(), is(statusCode));
   }

   @Then("the cart has {int} item with quantity {int}")
   public void verifyCartItems(int count, int qty) throws Exception {
      CartResponse cart = objectMapper.readValue(latestResponse.getBody(), CartResponse.class);
      assertThat(cart.items().size(), is(count));
      assertThat(cart.items().get(0).quantity(), is(qty));
   }

   @When("the client submits the cart for {string}")
   public void submit(String userId) {
      String targetId = (userId.equals("user123") || userId.contains("Profesor")) ? lastCreatedUserId : userId;
      latestResponse = restTemplate.postForEntity(BASE_URL + "/api/carts/" + targetId + "/submit", null, String.class);
   }

   @Then("the cart status is {string}")
   public void verifyStatus(String status) throws Exception {
      CartResponse cart = objectMapper.readValue(latestResponse.getBody(), CartResponse.class);
      assertThat(cart.status(), is(status));
   }
}