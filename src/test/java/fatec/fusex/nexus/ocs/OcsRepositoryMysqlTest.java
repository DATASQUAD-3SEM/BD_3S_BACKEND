package fatec.fusex.nexus.ocs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cobre o que o H2 nao consegue: busca sem diferenciar acentos (collation utf8mb4_unicode_ci do MySQL).
 *
 * So roda quando FUSEX_TEST_MYSQL=true e o MySQL do perfil dev esta de pe (docker compose up -d):
 *   FUSEX_TEST_MYSQL=true ./mvnw test -Dtest=OcsRepositoryMysqlTest
 * Sem a variavel, o teste e ignorado e nao quebra o build de quem so tem H2.
 * O @Transactional desfaz os dados no fim.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "FUSEX_TEST_MYSQL", matches = "true")
class OcsRepositoryMysqlTest {

    @Autowired
    private OcsRepository ocsRepository;

    private void salvar(String nome, String contrato) {
        Ocs ocs = new Ocs();
        ocs.setNome(nome);
        ocs.setContratoNum(contrato);
        ocs.setTipo(OcsTipo.OCS);
        ocsRepository.save(ocs);
    }

    private Page<Ocs> buscar(String termo) {
        return ocsRepository.buscarPorTermo(termo, PageRequest.of(0, 30, Sort.by("nome").ascending()));
    }

    @Test
    void buscaPorNome_ignoraAcentosNosDoisLados() {
        salvar("Ortopédia Teste Acento", "TESTE-ACENTO-1");

        assertThat(buscar("ortopedia teste").getContent()).extracting(Ocs::getNome)
                .contains("Ortopédia Teste Acento");
        assertThat(buscar("ORTOPÉDIA TESTE").getContent()).extracting(Ocs::getNome)
                .contains("Ortopédia Teste Acento");
    }
}
