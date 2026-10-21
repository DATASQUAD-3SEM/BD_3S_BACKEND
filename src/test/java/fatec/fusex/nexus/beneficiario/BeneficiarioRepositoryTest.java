package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.persistence.EntityManager;

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
}