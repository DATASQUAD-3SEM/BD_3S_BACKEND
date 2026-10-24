package fatec.fusex.nexus.correcaoMedico;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsTipo;

/**
 * Cobre o que o H2 nao consegue: busca sem diferenciar acentos (collation utf8mb4_unicode_ci do MySQL).
 *
 * So roda quando FUSEX_TEST_MYSQL=true e o MySQL do perfil dev esta de pe (docker compose up -d):
 *   FUSEX_TEST_MYSQL=true ./mvnw test -Dtest=OcsCorrecaoRepositoryMysqlTest
 * Sem a variavel, o teste e ignorado e nao quebra o build de quem so tem H2.
 * O @Transactional desfaz os dados no fim.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "FUSEX_TEST_MYSQL", matches = "true")
class OcsCorrecaoRepositoryMysqlTest {

    @Autowired
    private OcsCorrecaoRepository ocsRepository;

    @Test
    void buscaPorNome_ignoraAcentosNosDoisLados() {
        Ocs ocs = new Ocs();
        ocs.setNome("Clínica Médica Correção Teste");
        ocs.setContratoNum("TESTE-CORRECAO-1");
        ocs.setTipo(OcsTipo.OCS);
        ocsRepository.save(ocs);

        var pageable = PageRequest.of(0, 30, Sort.by("nome").ascending());
        assertThat(ocsRepository.buscarAtivasPorTermo("clinica medica correcao", pageable).getContent())
                .extracting(Ocs::getNome).contains("Clínica Médica Correção Teste");
        assertThat(ocsRepository.buscarAtivasPorTermo("CLÍNICA MÉDICA CORREÇÃO", pageable).getContent())
                .extracting(Ocs::getNome).contains("Clínica Médica Correção Teste");
    }
}
