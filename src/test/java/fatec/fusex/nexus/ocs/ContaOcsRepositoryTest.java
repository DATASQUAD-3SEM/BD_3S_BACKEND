package fatec.fusex.nexus.ocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Gravacao da ContaOcs no banco em memoria: a conta acha a sua OCS e ocs_id e unico (uma conta por OCS).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ContaOcsRepositoryTest {

    private final ContaOcsRepository repository;

    private final EntityManager em;

    ContaOcsRepositoryTest(ContaOcsRepository repository, EntityManager em) {
        this.repository = repository;
        this.em = em;
    }

    private Usuario usuarioOcs(String login) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(PerfilUsuario.OCS);
        em.persist(usuario);
        return usuario;
    }

    private Ocs ocs(String contrato) {
        Ocs ocs = new Ocs();
        ocs.setNome("Clinica " + contrato);
        ocs.setContratoNum(contrato);
        ocs.setTipo(OcsTipo.OCS);
        em.persist(ocs);
        return ocs;
    }

    private ContaOcs conta(Usuario usuario, Ocs ocs) {
        ContaOcs conta = new ContaOcs();
        conta.setUsuario(usuario);
        conta.setOcs(ocs);
        return conta;
    }

    @Test
    void salvaEBuscaContaComAOcsVinculada() {
        Usuario usuario = usuarioOcs("ocs.teste1");
        Ocs ocs = ocs("TESTE-CONTA-1");
        em.persist(conta(usuario, ocs));
        em.flush();
        em.clear();

        ContaOcs lida = repository.findById(usuario.getIdUsuario()).orElseThrow();

        assertThat(lida.getOcs().getIdOcs()).isEqualTo(ocs.getIdOcs());
    }

    @Test
    void mesmaOcsEmDuasContasFalha() {
        Ocs ocs = ocs("TESTE-CONTA-2");
        em.persist(conta(usuarioOcs("ocs.teste2a"), ocs));
        em.flush();

        assertThatThrownBy(() -> {
            em.persist(conta(usuarioOcs("ocs.teste2b"), ocs));
            em.flush();
        }).isInstanceOfAny(PersistenceException.class, DataIntegrityViolationException.class);
    }
}
