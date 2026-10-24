package fatec.fusex.nexus.ocs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Garante que a query de busca de OCS (nome, contrato e tipo) e valida e filtra certo. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OcsRepositoryTest {

    @Autowired
    private OcsRepository ocsRepository;

    @BeforeEach
    void dados() {
        salvar("Laboratorio Exemplo", "EXEMPLO-001", OcsTipo.OCS);
        salvar("Hospital Exemplo", "EXEMPLO-002", OcsTipo.PSA);
    }

    private void salvar(String nome, String contrato, OcsTipo tipo) {
        Ocs ocs = new Ocs();
        ocs.setNome(nome);
        ocs.setContratoNum(contrato);
        ocs.setTipo(tipo);
        ocsRepository.save(ocs);
    }

    private Page<Ocs> buscar(String termo) {
        return ocsRepository.buscarPorTermo(termo, PageRequest.of(0, 30, Sort.by("nome").ascending()));
    }

    @Test
    void buscaPorNome_ignoraMaiusculas() {
        assertThat(buscar("LABORA").getContent()).extracting(Ocs::getNome)
                .containsExactly("Laboratorio Exemplo");
    }

    @Test
    void buscaPorContrato() {
        assertThat(buscar("exemplo-002").getContent()).extracting(Ocs::getNome)
                .containsExactly("Hospital Exemplo");
    }

    @Test
    void buscaPorTipo() {
        assertThat(buscar("psa").getContent()).extracting(Ocs::getNome)
                .containsExactly("Hospital Exemplo");
    }

    @Test
    void semResultado_devolveVazioComTotalZero() {
        Page<Ocs> pagina = buscar("inexistente");
        assertThat(pagina.getContent()).isEmpty();
        assertThat(pagina.getTotalElements()).isZero();
    }

    @Test
    @Disabled("H2 não suporta collation accent-insensitive. Validar no MySQL (dev).")
    void buscaPorNome_ignoraAcentos() {
        salvar("Ortopédia Exemplo", "EXEMPLO-003", OcsTipo.OCS);
        assertThat(buscar("ortopedia").getContent()).extracting(Ocs::getNome)
                .containsExactly("Ortopédia Exemplo");
    }

    @Test
    void ocsInativa_naoApareceNaBusca() {
        Ocs inativa = new Ocs();
        inativa.setNome("Laboratorio Inativo");
        inativa.setContratoNum("EXEMPLO-999");
        inativa.setTipo(OcsTipo.OCS);
        inativa.setAtivo(false);
        ocsRepository.save(inativa);

        assertThat(buscar("inativo").getContent()).isEmpty();
    }
}
