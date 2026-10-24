package fatec.fusex.nexus.preGuia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsService;
import fatec.fusex.nexus.preGuia.dto.CorrecaoPreGuiaResponse;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class PreGuiaServiceCorrecaoTest {

    private static final String LOGIN = "52998224725";
    private static final LocalDateTime CRIADA_EM = LocalDateTime.of(2026, 9, 1, 10, 0);
    private static final LocalDateTime OBSERVADA_EM = LocalDateTime.of(2026, 9, 2, 14, 0);

    @Mock private PreGuiaRepository preGuiaRepository;
    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private OcsService ocsService;
    @Mock private ArmazenamentoArquivo armazenamentoArquivo;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PreGuiaHistoricoRepository historicoRepository;

    @InjectMocks
    private PreGuiaService preGuiaService;

    private Usuario usuario(Long id, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setLogin(LOGIN);
        u.setPerfil(perfil);
        return u;
    }

    private PreGuia preGuiaEmAtencaoDo(Long beneficiarioId) {
        Beneficiario b = new Beneficiario();
        b.setUsuarioId(beneficiarioId);
        b.setNome("Antonio Carlos Ferreira");
        b.setPrecCp("45872213300");

        Ocs ocs = new Ocs();
        ocs.setIdOcs(10L);
        ocs.setNome("Laboratorio Exemplo");

        PreGuia pg = new PreGuia();
        pg.setIdPreGuia(7L);
        pg.setBeneficiario(b);
        pg.setOcs(ocs);
        pg.setStatus(PreGuiaStatus.ATENCAO);
        pg.setDataCriacao(CRIADA_EM);
        pg.setEncaminhamentoUrl("encaminhamentos/abc-123.jpg");
        return pg;
    }

    private void logadoComo(PerfilUsuario perfil) {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario(1L, perfil)));
    }

    @Test
    void buscarDadosCorrecao_quandoSucesso_devolveDadosDaTela() {
        logadoComo(PerfilUsuario.BENEFICIARIO);
        PreGuia pg = preGuiaEmAtencaoDo(1L);
        when(preGuiaRepository.findById(7L)).thenReturn(Optional.of(pg));
        PreGuiaHistorico atencao = new PreGuiaHistorico(
                1L, pg, PreGuiaStatus.ATENCAO, "A foto enviada esta ilegivel.", null, null, OBSERVADA_EM);
        when(historicoRepository.findFirstByPreGuiaIdPreGuiaAndStatusOrderByDataHoraDesc(7L, PreGuiaStatus.ATENCAO))
                .thenReturn(Optional.of(atencao));
        when(historicoRepository.findFirstByPreGuiaIdPreGuiaAndEncaminhamentoUrlOrderByDataHoraDesc(
                7L, "encaminhamentos/abc-123.jpg")).thenReturn(Optional.empty());

        CorrecaoPreGuiaResponse resp = preGuiaService.buscarDadosCorrecao(7L, LOGIN);

        assertThat(resp.numero()).isEqualTo("0007");
        assertThat(resp.status()).isEqualTo("ATENCAO");
        assertThat(resp.beneficiario().nome()).isEqualTo("Antonio Carlos Ferreira");
        assertThat(resp.beneficiario().precCp()).isEqualTo("45872213300");
        assertThat(resp.dataCriacao()).isEqualTo(CRIADA_EM);
        assertThat(resp.observacao().texto()).isEqualTo("A foto enviada esta ilegivel.");
        assertThat(resp.observacao().dataRegistro()).isEqualTo(OBSERVADA_EM);
        assertThat(resp.ocsAtual().id()).isEqualTo(10L);
        assertThat(resp.ocsAtual().nome()).isEqualTo("Laboratorio Exemplo");
    }

    @Test
    void buscarDadosCorrecao_encaminhamentoDevolveSoONomeETipoSemOCaminho() {
        logadoComo(PerfilUsuario.BENEFICIARIO);
        when(preGuiaRepository.findById(7L)).thenReturn(Optional.of(preGuiaEmAtencaoDo(1L)));
        when(historicoRepository.findFirstByPreGuiaIdPreGuiaAndEncaminhamentoUrlOrderByDataHoraDesc(
                7L, "encaminhamentos/abc-123.jpg")).thenReturn(Optional.empty());

        CorrecaoPreGuiaResponse.EncaminhamentoResumo enc =
                preGuiaService.buscarDadosCorrecao(7L, LOGIN).encaminhamentoAtual();

        assertThat(enc.nome()).isEqualTo("abc-123.jpg");
        assertThat(enc.tipo()).isEqualTo("image/jpeg");
        // Sem linha no historico, a data de envio e a da criacao da pre-guia
        assertThat(enc.dataEnvio()).isEqualTo(CRIADA_EM);
    }

    @Test
    void buscarDadosCorrecao_encaminhamentoRegistradoNoHistorico_usaADataDoHistorico() {
        logadoComo(PerfilUsuario.BENEFICIARIO);
        PreGuia pg = preGuiaEmAtencaoDo(1L);
        pg.setEncaminhamentoUrl("encaminhamentos/novo.pdf");
        when(preGuiaRepository.findById(7L)).thenReturn(Optional.of(pg));
        LocalDateTime reenviadoEm = LocalDateTime.of(2026, 9, 3, 9, 30);
        PreGuiaHistorico envio = new PreGuiaHistorico(
                2L, pg, PreGuiaStatus.PENDENTE, null, "encaminhamentos/novo.pdf", null, reenviadoEm);
        when(historicoRepository.findFirstByPreGuiaIdPreGuiaAndEncaminhamentoUrlOrderByDataHoraDesc(
                7L, "encaminhamentos/novo.pdf")).thenReturn(Optional.of(envio));

        CorrecaoPreGuiaResponse.EncaminhamentoResumo enc =
                preGuiaService.buscarDadosCorrecao(7L, LOGIN).encaminhamentoAtual();

        assertThat(enc.tipo()).isEqualTo("application/pdf");
        assertThat(enc.dataEnvio()).isEqualTo(reenviadoEm);
    }

    @Test
    void buscarDadosCorrecao_quandoStatusDiferenteDeAtencao_lancaConflito() {
        logadoComo(PerfilUsuario.BENEFICIARIO);
        PreGuia pg = preGuiaEmAtencaoDo(1L);
        pg.setStatus(PreGuiaStatus.PENDENTE);
        when(preGuiaRepository.findById(7L)).thenReturn(Optional.of(pg));

        assertThatThrownBy(() -> preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void buscarDadosCorrecao_quandoPreGuiaDeOutroBeneficiario_lancaNaoEncontrado() {
        logadoComo(PerfilUsuario.BENEFICIARIO);
        when(preGuiaRepository.findById(7L)).thenReturn(Optional.of(preGuiaEmAtencaoDo(99L)));

        assertThatThrownBy(() -> preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarDadosCorrecao_quandoPreGuiaNaoExiste_lancaNaoEncontrado() {
        logadoComo(PerfilUsuario.BENEFICIARIO);
        when(preGuiaRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarDadosCorrecao_quandoOrigemMedico_lancaAcessoNegado() {
        logadoComo(PerfilUsuario.BENEFICIARIO);
        PreGuia pg = preGuiaEmAtencaoDo(1L);
        pg.setMedicoFusex(new MedicoFusex());
        when(preGuiaRepository.findById(7L)).thenReturn(Optional.of(pg));

        assertThatThrownBy(() -> preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("medico");
    }

    @Test
    void buscarDadosCorrecao_quandoOrigemMedicoEStatusDiferente_prevaleceO403() {
        logadoComo(PerfilUsuario.BENEFICIARIO);
        PreGuia pg = preGuiaEmAtencaoDo(1L);
        pg.setMedicoFusex(new MedicoFusex());
        pg.setStatus(PreGuiaStatus.PENDENTE);
        when(preGuiaRepository.findById(7L)).thenReturn(Optional.of(pg));

        assertThatThrownBy(() -> preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void buscarDadosCorrecao_quandoPerfilErrado_lancaAcessoNegadoSemBuscarPreGuia() {
        logadoComo(PerfilUsuario.MEDICO);

        assertThatThrownBy(() -> preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .isInstanceOf(AcessoNegadoException.class);
        verify(preGuiaRepository, never()).findById(any());
    }

    @Test
    void buscarDadosCorrecao_quandoUsuarioNaoExiste_lancaNaoAutenticado() {
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .isInstanceOf(NaoAutenticadoException.class);
    }
}
