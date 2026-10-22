package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BeneficiarioRepositoryTest {

    @Autowired
    private BeneficiarioRepository repository;

    @Autowired
    private EntityManager em;

    @Test
    void salvaEBuscaPorCpfEPrecCp() {
        Usuario usuario = new Usuario();
        usuario.setLogin("12345678901");
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        em.persist(usuario);

        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome("Fulano de Tal");
        b.setPrecCp("0001112223");
        b.setDataNascimento(LocalDate.of(1968, 3, 15));
        b.setTelefone("12987654321");
        em.persist(b);
        em.flush();

        assertThat(repository.findByCpf("12345678901")).isPresent();
        assertThat(repository.existsByPrecCp("0001112223")).isTrue();
        assertThat(repository.existsByCpf("00000000000")).isFalse();
    }

    @Test
    void findWithUsuarioByPrecCp_carregaOUsuarioJuntoComOBeneficiario() {
        Usuario usuario = new Usuario();
        usuario.setLogin("98765432100");
        usuario.setSenhaHash("hash-de-teste");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuario.setAtivo(false);
        em.persist(usuario);

        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome("Beltrano");
        b.setPrecCp("45872213300");
        b.setDataNascimento(LocalDate.of(1990, 1, 1));
        b.setTelefone("12987654321");
        em.persist(b);
        em.flush();
        em.clear(); // forca ler do banco de novo, sem reaproveitar o objeto da sessao

        Beneficiario achado = repository.findWithUsuarioByPrecCp("45872213300").orElseThrow();

        assertThat(Hibernate.isInitialized(achado.getUsuario())).isTrue();
        assertThat(achado.getUsuario().getAtivo()).isFalse();
        assertThat(repository.findWithUsuarioByPrecCp("00000000000")).isEmpty();
    }
}