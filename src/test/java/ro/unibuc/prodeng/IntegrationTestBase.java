package ro.unibuc.prodeng;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Tag("IntegrationTest")
public abstract class IntegrationTestBase {

    private static final TransitionWalker.ReachedState<RunningMongodProcess> MONGOD;

    static {
        MONGOD = Mongod.instance().start(Version.Main.V7_0);
        Runtime.getRuntime().addShutdownHook(new Thread(MONGOD::close));
    }

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        var addr = MONGOD.current().getServerAddress();
        registry.add("mongodb.connection.url", () -> "mongodb://" + addr.getHost() + ":" + addr.getPort());
    }
}
