package fatec.fusex.nexus.preguia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;

@ExtendWith(MockitoExtension.class)
class PreGuiaServiceTest {

    @Mock
    private PreGuiaRepository preGuiaRepository;

    @Mock
    private BeneficiarioRepository beneficiarioRepository;

    @InjectMocks
    private PreGuiaService preGuiaService;

    /** Cria um Beneficiario com id setado (o id nao tem setter na entidade). */
    private Beneficiario beneficiarioComId(Long id) {
        Beneficiario b = new Beneficiario();
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    private PreGuia preGuiaDo(Beneficiario dono) {
        PreGuia pg = new PreGuia();
        pg.setBeneficiario(dono);
        return pg;
    }

    // ---------- listarDoBeneficiario ----------

    @Test
    void listarDoBeneficiario_quandoBeneficiarioExiste_devolveLista() {
        Beneficiario b = beneficiarioComId(1L);
        PreGuia pg = preGuiaDo(b);

        when(beneficiarioRepository.existsById(1L)).thenReturn(true);
        when(preGuiaRepository.findByBeneficiarioId(1L)).thenReturn(List.of(pg));

        List<PreGuia> resultado = preGuiaService.listarDoBeneficiario(1L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    void listarDoBeneficiario_quandoBeneficiarioNaoExiste_lancaExcecao() {
        when(beneficiarioRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> preGuiaService.listarDoBeneficiario(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }

    // ---------- buscarDoBeneficiario ----------

    @Test
    void buscarDoBeneficiario_quandoVinculoBate_devolvePreGuia() {
        Beneficiario dono = beneficiarioComId(1L);
        PreGuia pg = preGuiaDo(dono);

        when(preGuiaRepository.findById(10L)).thenReturn(Optional.of(pg));

        PreGuia resultado = preGuiaService.buscarDoBeneficiario(10L, 1L);

        assertThat(resultado.getBeneficiario().getId()).isEqualTo(1L);
    }

    @Test
    void buscarDoBeneficiario_quandoVinculoNaoBate_lancaExcecao() {
        // Pre-guia e do beneficiario 1, mas quem pediu e o 2.
        Beneficiario dono = beneficiarioComId(1L);
        PreGuia pg = preGuiaDo(dono);

        when(preGuiaRepository.findById(10L)).thenReturn(Optional.of(pg));

        assertThatThrownBy(() -> preGuiaService.buscarDoBeneficiario(10L, 2L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("10");
    }

    @Test
    void buscarDoBeneficiario_quandoPreGuiaNaoExiste_lancaExcecao() {
        when(preGuiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> preGuiaService.buscarDoBeneficiario(99L, 1L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}