package fatec.fusex.nexus.espelho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * T28-BE3 (V21): o banco garante um espelho em construcao por OCS (uk_espelho_ocs_em_construcao) e
 * GuiaProcedimento nao tem mais valor_unitario. Cobre o que o H2 nao reproduz (coluna gerada do MySQL).
 *
 * So roda quando FUSEX_TEST_MYSQL=true e o MySQL do perfil dev esta de pe (docker compose up -d):
 *   FUSEX_TEST_MYSQL=true ./mvnw test -Dtest=EspelhoEmConstrucaoUnicoMysqlTest
 * O @Transactional desfaz os dados no fim.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "FUSEX_TEST_MYSQL", matches = "true")
class EspelhoEmConstrucaoUnicoMysqlTest {

    private final JdbcTemplate jdbc;

    EspelhoEmConstrucaoUnicoMysqlTest(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private long novaOcs(String contrato) {
        jdbc.update("INSERT INTO Ocs (contrato_num, nome, tipo, ativo) VALUES (?, 'OCS Teste V21', 'OCS', 1)", contrato);
        return jdbc.queryForObject("SELECT id_ocs FROM Ocs WHERE contrato_num = ?", Long.class, contrato);
    }

    private void espelho(long ocsId, String status) {
        long usuario = jdbc.queryForObject("SELECT MIN(id_usuario) FROM Usuario", Long.class);
        if ("ENVIADO".equals(status)) {
            jdbc.update("INSERT INTO Espelho (ocs_id, status, criado_por, data_envio, enviado_por) "
                    + "VALUES (?, 'ENVIADO', ?, NOW(), ?)", ocsId, usuario, usuario);
        } else {
            jdbc.update("INSERT INTO Espelho (ocs_id, status, criado_por) VALUES (?, 'EM_CONSTRUCAO', ?)", ocsId, usuario);
        }
    }

    @Test
    void segundoEspelhoEmConstrucaoDaMesmaOcs_eRecusado() {
        long ocs = novaOcs("TESTE-V21-1");
        espelho(ocs, "EM_CONSTRUCAO");

        assertThatThrownBy(() -> espelho(ocs, "EM_CONSTRUCAO")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void variosEspelhosEnviadosDaMesmaOcs_saoAceitos() {
        long ocs = novaOcs("TESTE-V21-2");
        espelho(ocs, "ENVIADO");
        espelho(ocs, "ENVIADO");
        espelho(ocs, "EM_CONSTRUCAO");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM Espelho WHERE ocs_id = ?", Integer.class, ocs))
                .isEqualTo(3);
    }

    @Test
    void guiaProcedimentoNaoTemValorUnitario() {
        Integer colunas = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'GuiaProcedimento' "
                + "AND COLUMN_NAME = 'valor_unitario'", Integer.class);

        assertThat(colunas).isZero();
    }
}
