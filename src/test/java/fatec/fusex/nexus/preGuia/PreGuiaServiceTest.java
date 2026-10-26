package fatec.fusex.nexus.preGuia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.common.validation.ValidacaoEncaminhamento;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.preGuia.dto.PreGuiaResponse;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Testes da criacao da pre-guia pelo beneficiario logado: perfil, OCS ativa, arquivo valido,
 * primeira linha do historico e remocao do arquivo quando o banco falha.
 */
@ExtendWith(MockitoExtension.class)
class PreGuiaServiceTest {

    @Mock private PreGuiaRepository preGuiaRepository;
    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private OcsRepository ocsRepository;
    @Mock private PreGuiaHistoricoRepository historicoRepository;
    @Mock private ArmazenamentoArquivo armazenamentoArquivo;
    @Mock private UsuarioLogado usuarioLogado;

    private PreGuiaService preGuiaService;

    private final MockMultipartFile pdf =
            new MockMultipartFile("arquivo", "encaminhamento.pdf", "application/pdf", "%PDF-1.4 conteudo".getBytes());

    private Usuario usuario;

    @BeforeEach
    void preparar() {
        preGuiaService = new PreGuiaService(preGuiaRepository, beneficiarioRepository, ocsRepository,
                historicoRepository, armazenamentoArquivo, usuarioLogado, new ValidacaoEncaminhamento(10_485_760));
        usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuario.setAtivo(true);
    }

    private Beneficiario beneficiarioComId(Long usuarioId) {
        Beneficiario b = new Beneficiario();
        b.setUsuarioId(usuarioId);
        b.setPrecCp("0001112223");
        return b;
    }

    private PreGuia preGuiaDo(Beneficiario dono) {
        PreGuia pg = new PreGuia();
        pg.setBeneficiario(dono);
        return pg;
    }

    private Ocs ocs(Long id, boolean ativa) {
        Ocs ocs = new Ocs();
        ocs.setIdOcs(id);
        ocs.setAtivo(ativa);
        return ocs;
    }

    private void beneficiarioLogado() {
        when(usuarioLogado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO)).thenReturn(usuario);
        when(beneficiarioRepository.findById(1L)).thenReturn(Optional.of(beneficiarioComId(1L)));
    }

    @Test
    void listarDoBeneficiario_quandoBeneficiarioExiste_devolveLista() {
        Beneficiario b = beneficiarioComId(1L);
        when(beneficiarioRepository.existsById(1L)).thenReturn(true);
        when(preGuiaRepository.findByBeneficiarioUsuarioId(1L)).thenReturn(List.of(preGuiaDo(b)));

        assertThat(preGuiaService.listarDoBeneficiario(1L)).hasSize(1);
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
        when(preGuiaRepository.findById(10L)).thenReturn(Optional.of(preGuiaDo(beneficiarioComId(1L))));

        assertThat(preGuiaService.buscarDoBeneficiario(10L, 1L).getBeneficiario().getUsuarioId()).isEqualTo(1L);
    }

    @Test
    void buscarDoBeneficiario_quandoVinculoNaoBate_lancaExcecao() {
        when(preGuiaRepository.findById(10L)).thenReturn(Optional.of(preGuiaDo(beneficiarioComId(1L))));

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

    @Test
    void criar_quandoTudoValido_criaPreGuiaPendenteEGravaOHistorico() {
        beneficiarioLogado();
        when(ocsRepository.findById(10L)).thenReturn(Optional.of(ocs(10L, true)));
        when(armazenamentoArquivo.salvar(any(), anyString())).thenReturn("encaminhamentos/gerado.pdf");
        when(preGuiaRepository.save(any())).thenAnswer(inv -> {
            PreGuia salva = inv.getArgument(0);
            salva.setIdPreGuia(7L);
            return salva;
        });

        PreGuiaResponse resultado = preGuiaService.criar(10L, pdf);

        assertThat(resultado.numero()).isEqualTo("0007");
        assertThat(resultado.status()).isEqualTo("PENDENTE");
        assertThat(resultado.ocsId()).isEqualTo(10L);

        ArgumentCaptor<PreGuiaHistorico> historico = ArgumentCaptor.forClass(PreGuiaHistorico.class);
        verify(historicoRepository).save(historico.capture());
        assertThat(historico.getValue().getStatus()).isEqualTo(PreGuiaStatus.PENDENTE);
        assertThat(historico.getValue().getUsuario()).isSameAs(usuario);
        assertThat(historico.getValue().getEncaminhamentoUrl()).isEqualTo("encaminhamentos/gerado.pdf");
    }

    @Test
    void criar_perfilErrado_lanca403SemGravarNada() {
        when(usuarioLogado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO))
                .thenThrow(new AcessoNegadoException("Acesso negado"));

        assertThatThrownBy(() -> preGuiaService.criar(10L, pdf)).isInstanceOf(AcessoNegadoException.class);
        verify(armazenamentoArquivo, never()).salvar(any(), anyString());
    }

    @Test
    void criar_semOcs_lanca422NoCampoOcsId() {
        beneficiarioLogado();

        assertThatThrownBy(() -> preGuiaService.criar(null, pdf))
                .isInstanceOf(DadosInvalidosException.class)
                .extracting(e -> ((DadosInvalidosException) e).getCampo())
                .isEqualTo("ocsId");
    }

    @Test
    void criar_ocsInativa_lanca422NoCampoOcsId() {
        beneficiarioLogado();
        when(ocsRepository.findById(5L)).thenReturn(Optional.of(ocs(5L, false)));

        assertThatThrownBy(() -> preGuiaService.criar(5L, pdf))
                .isInstanceOf(DadosInvalidosException.class)
                .extracting(e -> ((DadosInvalidosException) e).getCampo())
                .isEqualTo("ocsId");
    }

    @Test
    void criar_semArquivo_lanca422NoCampoArquivo() {
        beneficiarioLogado();
        when(ocsRepository.findById(10L)).thenReturn(Optional.of(ocs(10L, true)));

        assertThatThrownBy(() -> preGuiaService.criar(10L, new MockMultipartFile("arquivo", new byte[0])))
                .isInstanceOf(DadosInvalidosException.class)
                .extracting(e -> ((DadosInvalidosException) e).getCampo())
                .isEqualTo("arquivo");
    }

    @Test
    void criar_arquivoComFormatoInvalido_lanca422() {
        beneficiarioLogado();
        when(ocsRepository.findById(10L)).thenReturn(Optional.of(ocs(10L, true)));
        MockMultipartFile falso = new MockMultipartFile("arquivo", "foto.jpg", "image/jpeg", "nao e imagem".getBytes());

        assertThatThrownBy(() -> preGuiaService.criar(10L, falso)).isInstanceOf(DadosInvalidosException.class);
        verify(armazenamentoArquivo, never()).salvar(any(), anyString());
    }

    @Test
    void criar_quandoSaveFalha_removeArquivoSalvo() {
        beneficiarioLogado();
        when(ocsRepository.findById(10L)).thenReturn(Optional.of(ocs(10L, true)));
        when(armazenamentoArquivo.salvar(any(), anyString())).thenReturn("encaminhamentos/orfao.pdf");
        when(preGuiaRepository.save(any())).thenThrow(new RuntimeException("constraint"));

        assertThatThrownBy(() -> preGuiaService.criar(10L, pdf)).isInstanceOf(RuntimeException.class);

        verify(armazenamentoArquivo).removerSemFalhar("encaminhamentos/orfao.pdf");
    }
}
