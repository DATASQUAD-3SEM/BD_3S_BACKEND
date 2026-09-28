package fatec.fusex.nexus.preguia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsService;
import fatec.fusex.nexus.preguia.dto.PreGuiaResponse;

@ExtendWith(MockitoExtension.class)
class PreGuiaServiceTest {

    @Mock private PreGuiaRepository preGuiaRepository;
    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private OcsService ocsService;
    @Mock private ArmazenamentoArquivo armazenamentoArquivo;

    @InjectMocks
    private PreGuiaService preGuiaService;

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

    private Ocs ocsComId(Long id) {
        Ocs ocs = new Ocs();
        ReflectionTestUtils.setField(ocs, "id", id);
        return ocs;
    }

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

    private final MockMultipartFile arquivo =
            new MockMultipartFile("arquivo", "encaminhamento.pdf", "application/pdf", "conteudo".getBytes());

    @Test
    void criar_quandoTudoValido_criaPreGuiaPendente() {
        Beneficiario beneficiario = beneficiarioComId(1L);
        beneficiario.setCpf("12345678901");
        beneficiario.setPrecCp("0001112223");
        Ocs ocs = ocsComId(10L);

        when(ocsService.buscarPorId(10L)).thenReturn(ocs);
        when(beneficiarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(beneficiario));
        when(armazenamentoArquivo.salvar(any(), anyString())).thenReturn("encaminhamentos/gerado.pdf");

        PreGuiaResponse resultado = preGuiaService.criar("12345678901", "0001112223", 10L, arquivo);

        assertThat(resultado.status()).isEqualTo("PENDENTE");
        assertThat(resultado.encaminhamentoUrl()).isEqualTo("encaminhamentos/gerado.pdf");
        assertThat(resultado.ocsId()).isEqualTo(10L);
    }

    @Test
    void criar_quandoArquivoAusente_lancaRegraDeNegocio() {
        MockMultipartFile vazio = new MockMultipartFile("arquivo", new byte[0]);

        assertThatThrownBy(() -> preGuiaService.criar("12345678901", "0001112223", 10L, vazio))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("encaminhamento");
    }

    @Test
    void criar_quandoOcsNaoExiste_propagaExcecaoDoOcsService() {
        when(ocsService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("OCS nao encontrada: id 99"));

        assertThatThrownBy(() -> preGuiaService.criar("12345678901", "0001112223", 99L, arquivo))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void criar_quandoSaveFalha_removeArquivoSalvo() {
        Beneficiario beneficiario = beneficiarioComId(1L);
        beneficiario.setCpf("12345678901");
        beneficiario.setPrecCp("0001112223");
        Ocs ocs = ocsComId(10L);

        when(ocsService.buscarPorId(10L)).thenReturn(ocs);
        when(beneficiarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(beneficiario));
        when(armazenamentoArquivo.salvar(any(), anyString())).thenReturn("encaminhamentos/orfao.pdf");
        when(preGuiaRepository.save(any())).thenThrow(new RuntimeException("constraint"));

        assertThatThrownBy(() -> preGuiaService.criar("12345678901", "0001112223", 10L, arquivo))
                .isInstanceOf(RuntimeException.class);

        verify(armazenamentoArquivo).remover("encaminhamentos/orfao.pdf");
    }

    @Test
    void criar_quandoBeneficiarioNaoExiste_lancaRecursoNaoEncontrado() {
        Ocs ocs = ocsComId(10L);

        when(ocsService.buscarPorId(10L)).thenReturn(ocs);
        when(beneficiarioRepository.findByCpf("00000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> preGuiaService.criar("00000000000", "0001112223", 10L, arquivo))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void criar_quandoPrecCpNaoConfereComOCpf_lancaRegraDeNegocio() {
        Beneficiario beneficiario = beneficiarioComId(1L);
        beneficiario.setCpf("12345678901");
        beneficiario.setPrecCp("0001112223");
        Ocs ocs = ocsComId(10L);

        when(ocsService.buscarPorId(10L)).thenReturn(ocs);
        when(beneficiarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(beneficiario));

        assertThatThrownBy(() -> preGuiaService.criar("12345678901", "precCp-errado", 10L, arquivo))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Prec-CP");
    }
}
