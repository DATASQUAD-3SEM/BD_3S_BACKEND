package fatec.fusex.nexus.procedimentoExame;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.ocsProcedimento.OcsProcedimento;
import fatec.fusex.nexus.ocsProcedimento.OcsProcedimentoId;
import jakarta.persistence.EntityManager;

/** Garante que a query de busca de procedimentos (nome e codigo TUSS) e valida e filtra certo. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProcedimentoExameRepositoryTest {

    @Autowired
    private ProcedimentoExameRepository procedimentoRepository;

    @Autowired
    private OcsRepository ocsRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void dados() {
        salvar("40304361", "Hemograma com contagem de plaquetas ou estimativa");
        salvar("40302040", "Glicose");
        salvar("40301630", "Colesterol total");
    }

    private ProcedimentoExame salvar(String codigoTuss, String nome) {
        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss(codigoTuss);
        p.setTerminologiaProcedimento(nome);
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        return procedimentoRepository.save(p);
    }

    private List<ProcedimentoExame> buscar(String termo) {
        return procedimentoRepository.buscarPorTermo(termo,
                PageRequest.of(0, 30, Sort.by("terminologiaProcedimento").ascending()));
    }

    @Test
    void buscaPorNome_ignoraMaiusculas() {
        assertThat(buscar("GLIC")).extracting(ProcedimentoExame::getTerminologiaProcedimento)
                .containsExactly("Glicose");
    }

    @Test
    void buscaPorCodigoTuss_aceitaParteDoCodigo() {
        assertThat(buscar("403016")).extracting(ProcedimentoExame::getCodigoTuss)
                .containsExactly("40301630");
    }

    @Test
    void buscaPorCodigoComum_devolveEmOrdemAlfabetica() {
        assertThat(buscar("403")).extracting(ProcedimentoExame::getTerminologiaProcedimento)
                .containsExactly("Colesterol total", "Glicose", "Hemograma com contagem de plaquetas ou estimativa");
    }

    @Test
    void procedimentoDeQualquerOcs_aparece() {
        // Hemograma ligado a uma OCS; Glicose sem nenhuma OCS: os dois aparecem
        Ocs ocs = new Ocs();
        ocs.setNome("Hospital Exemplo");
        ocs.setContratoNum("EXEMPLO-002");
        ocs.setTipo(OcsTipo.PSA);
        ocsRepository.save(ocs);
        ProcedimentoExame hemograma = procedimentoRepository.findByCodigoTuss("40304361").get(0);
        entityManager.persist(new OcsProcedimento(
                new OcsProcedimentoId(ocs.getIdOcs(), hemograma.getIdProcedimentoExame()), ocs, hemograma));

        assertThat(buscar("o")).extracting(ProcedimentoExame::getCodigoTuss)
                .contains("40304361", "40302040");
    }

    @Test
    void semResultado_devolveVazio() {
        assertThat(buscar("inexistente")).isEmpty();
    }

    @Test
    void limiteDe30() {
        for (int i = 1; i <= 35; i++) {
            salvar("9" + String.format("%07d", i), "Massa " + String.format("%02d", i));
        }

        List<ProcedimentoExame> resultado = buscar("massa");

        assertThat(resultado).hasSize(30);
        assertThat(resultado.get(0).getTerminologiaProcedimento()).isEqualTo("Massa 01");
    }

    @Test
    void curingaEscapado_naoCasaComTudo() {
        // "!%" e o '%' escapado pelo ProcedimentoExameService: so casa com um '%' literal no texto
        assertThat(buscar("!%")).isEmpty();

        salvar("40399999", "Dosagem 100% teste");
        assertThat(buscar("100!%")).extracting(ProcedimentoExame::getTerminologiaProcedimento)
                .containsExactly("Dosagem 100% teste");
    }
}
