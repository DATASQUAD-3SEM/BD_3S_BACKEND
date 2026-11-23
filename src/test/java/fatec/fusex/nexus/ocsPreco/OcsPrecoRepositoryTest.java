package fatec.fusex.nexus.ocsPreco;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsTipo;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OcsPrecoRepositoryTest {

    private final OcsPrecoRepository repository;

    private final EntityManager em;

    OcsPrecoRepositoryTest(OcsPrecoRepository repository, EntityManager em) {
        this.repository = repository;
        this.em = em;
    }

    private Ocs ocs(String contrato) {
        Ocs ocs = new Ocs();
        ocs.setNome("Laboratorio " + contrato);
        ocs.setContratoNum(contrato);
        ocs.setTipo(OcsTipo.OCS);
        em.persist(ocs);
        return ocs;
    }

    private OcsPreco preco(Ocs ocs, String codDgp, String valor) {
        OcsPreco preco = new OcsPreco();
        preco.setOcs(ocs);
        preco.setCodDgp(codDgp);
        preco.setValorSessao(new BigDecimal(valor));
        return preco;
    }

    @Test
    void buscaPrecoPelaOcsEPeloCodigo() {
        Ocs ocs = ocs("CTR-PRECO-1");
        em.persist(preco(ocs, "41001109", "100.00"));
        em.flush();
        em.clear();

        OcsPreco lido = repository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), "41001109").orElseThrow();

        assertThat(lido.getValorSessao()).isEqualByComparingTo("100.00");
        assertThat(lido.getAtualizadoEm()).isNull();
        assertThat(lido.getAtualizadoPor()).isNull();
    }

    @Test
    void codigoSemPrecoNaoRetornaRegistro() {
        Ocs ocs = ocs("CTR-PRECO-2");
        em.persist(preco(ocs, "41001109", "100.00"));
        em.flush();
        em.clear();

        assertThat(repository.findByOcsIdOcsAndCodDgp(ocs.getIdOcs(), "41001110")).isEmpty();
    }

    @Test
    void mesmoCodigoNaMesmaOcsFalha() {
        Ocs ocs = ocs("CTR-PRECO-3");
        em.persist(preco(ocs, "41001109", "100.00"));
        em.flush();

        assertThatThrownBy(() -> {
            em.persist(preco(ocs, "41001109", "120.00"));
            em.flush();
        }).isInstanceOfAny(PersistenceException.class, DataIntegrityViolationException.class);
    }

    @Test
    void mesmoCodigoEmOutraOcsEAceito() {
        Ocs primeira = ocs("CTR-PRECO-4");
        Ocs segunda = ocs("CTR-PRECO-5");
        em.persist(preco(primeira, "41001109", "100.00"));
        em.persist(preco(segunda, "41001109", "90.00"));
        em.flush();
        em.clear();

        OcsPreco lido = repository.findByOcsIdOcsAndCodDgp(segunda.getIdOcs(), "41001109").orElseThrow();

        assertThat(lido.getValorSessao()).isEqualByComparingTo("90.00");
    }
}