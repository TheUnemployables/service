package ro.unibuc.prodeng;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@Tag("IntegrationTest") // obligatoriu integration test-urile să aibă acest tag pentru a fi rulate în Jenkins
public abstract class IntegrationTestBase {

   private static final MongoDBContainer mongoDBContainer =
           new MongoDBContainer("mongo:6.0.20")
                   .withExposedPorts(27017)
                   .withSharding()
                   .withLabel("ro.unibuc.prodeng", "integration-test-mongo");

   static {
      // Dacă nu avem deja o conexiune setată (ex: în Jenkins), pornim containerul local
      if (System.getenv("MONGODB_CONNECTION_URL") == null) {
          mongoDBContainer.start();
      }
   }

   @DynamicPropertySource
   static void setProperties(DynamicPropertyRegistry registry) {
      // Dacă am pornit containerul local, mapam portul dinamic
      if (mongoDBContainer.isRunning()) {
          String mongoUrl = "mongodb://localhost:" + mongoDBContainer.getMappedPort(27017);
          registry.add("mongodb.connection.url", () -> mongoUrl);
      }
      // Altfel, presupunem că avem deja o conexiune setată (ex: în Jenkins) și nu facem nimic
    }
}