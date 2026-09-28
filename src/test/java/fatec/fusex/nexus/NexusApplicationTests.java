package fatec.fusex.nexus;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste mais importante do projeto: se a aplicacao SOBE (Spring + Flyway + JPA + H2), passa.
 * Se ele falhar depois do seu push, provavelmente sua migration ou entidade esta errada.
 */
@SpringBootTest
@ActiveProfiles("test")
class NexusApplicationTests {

    @Test
    void contextLoads() {
    }
}
