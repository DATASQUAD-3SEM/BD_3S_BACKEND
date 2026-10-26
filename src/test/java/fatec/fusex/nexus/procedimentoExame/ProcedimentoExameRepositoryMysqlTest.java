package fatec.fusex.nexus.procedimentoExame;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cobre o que o H2 nao consegue: busca sem diferenciar acentos (collation utf8mb4_unicode_ci do MySQL).
 *
 * So roda quando FUSEX_TEST_MYSQL=true e o MySQL do perfil dev esta de pe (docker compose up -d):
 *   FUSEX_TEST_MYSQL=true ./mvnw test -Dtest=ProcedimentoExameRepositoryMysqlTest
 * Sem a variavel, o teste e ignorado e nao quebra o build de quem so tem H2.
 * O @Transactional desfaz os dados no fim.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "FUSEX_TEST_MYSQL", matches = "true")
class ProcedimentoExameRepositoryMysqlTest {

    private final ProcedimentoExameRepository procedimentoRepository;

    private final JdbcTemplate jdbcTemplate;

    ProcedimentoExameRepositoryMysqlTest(ProcedimentoExameRepository procedimentoRepository, JdbcTemplate jdbcTemplate) {
        this.procedimentoRepository = procedimentoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    private List<ProcedimentoExame> buscar(String termo) {
        return procedimentoRepository.buscarPorTermo(termo,
                PageRequest.of(0, 30, Sort.by("terminologiaProcedimento").ascending()));
    }

    @Test
    void buscaPorNome_ignoraAcentosNosDoisLados() {
        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss("99999901");
        p.setTerminologiaProcedimento("Ácido Teste Acento");
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        procedimentoRepository.save(p);

        assertThat(buscar("acido teste")).extracting(ProcedimentoExame::getTerminologiaProcedimento)
                .contains("Ácido Teste Acento");
        assertThat(buscar("ÁCIDO TESTE")).extracting(ProcedimentoExame::getTerminologiaProcedimento)
                .contains("Ácido Teste Acento");
    }

    @Test
    void colunasDeTexto_usamCollationQueIgnoraAcentoEMaiuscula() {
        // Garante a migration V5: as colunas pesquisadas precisam ser utf8mb4_unicode_ci (accent e case insensitive)
        var collations = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME, COLLATION_NAME FROM information_schema.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ProcedimentoExame' "
                        + "AND COLUMN_NAME IN ('codigo_tuss', 'terminologia_procedimento')");

        assertThat(collations).hasSize(2);
        assertThat(collations).allSatisfy(coluna ->
                assertThat(coluna.get("COLLATION_NAME")).isEqualTo("utf8mb4_unicode_ci"));
    }
}
