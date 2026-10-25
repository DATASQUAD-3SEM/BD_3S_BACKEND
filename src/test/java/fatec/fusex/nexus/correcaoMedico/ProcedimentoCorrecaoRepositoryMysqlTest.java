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

import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRolAns;

/**
 * Cobre o que o H2 nao consegue: busca sem diferenciar acentos (collation utf8mb4_unicode_ci do MySQL).
 *
 * So roda quando FUSEX_TEST_MYSQL=true e o MySQL do perfil dev esta de pe (docker compose up -d):
 *   FUSEX_TEST_MYSQL=true ./mvnw test -Dtest=ProcedimentoCorrecaoRepositoryMysqlTest
 * Sem a variavel, o teste e ignorado e nao quebra o build de quem so tem H2.
 * O @Transactional desfaz os dados no fim.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@EnabledIfEnvironmentVariable(named = "FUSEX_TEST_MYSQL", matches = "true")
class ProcedimentoCorrecaoRepositoryMysqlTest {

    @Autowired
    private ProcedimentoCorrecaoRepository procedimentoRepository;

    @Test
    void buscaPorNome_ignoraAcentosNosDoisLados() {
        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss("99999902");
        p.setTerminologiaProcedimento("Dosagem Ácido Correção Teste");
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        procedimentoRepository.save(p);

        var pageable = PageRequest.of(0, 30, Sort.by("terminologiaProcedimento").ascending());
        assertThat(procedimentoRepository.buscarNoCatalogo("acido correcao", pageable))
                .extracting(ProcedimentoExame::getTerminologiaProcedimento)
                .contains("Dosagem Ácido Correção Teste");
        assertThat(procedimentoRepository.buscarNoCatalogo("ÁCIDO CORREÇÃO", pageable))
                .extracting(ProcedimentoExame::getTerminologiaProcedimento)
                .contains("Dosagem Ácido Correção Teste");
    }
}
