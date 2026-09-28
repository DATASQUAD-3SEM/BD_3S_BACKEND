package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * MODELO DE TESTE COM BANCO (H2). @Transactional = tudo que o teste grava e desfeito no final.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BeneficiarioRepositoryTest {

    @Autowired
    private BeneficiarioRepository repository;

    @Test
    void salvaEBuscaPorCpfEPrecCp() {
        Beneficiario b = new Beneficiario();
        b.setCpf("12345678901");
        b.setPrecCp("0001112223");
        b.setNome("Fulano de Tal");
        b.setIdade(40);
        b.setSenha("hash-de-teste");
        repository.saveAndFlush(b);

        assertThat(repository.findByCpf("12345678901")).isPresent();
        assertThat(repository.existsByPrecCp("0001112223")).isTrue();
        assertThat(repository.existsByCpf("00000000000")).isFalse();
    }
}
