package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.dto.AssinaturaRegistradaResponse;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistorico;
import fatec.fusex.nexus.preGuiaHistorico.PreGuiaHistoricoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

class RegistrarAssinaturaGuiaServiceTest {

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private AssinaturaGuiaRepository assinaturaGuiaRepository;

    @Mock
    private PreGuiaHistoricoRepository historicoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    private RegistrarAssinaturaGuiaService service;
    private PreGuia preGuia;
    private Guia guia;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new RegistrarAssinaturaGuiaService(guiaRepository, assinaturaGuiaRepository,
                historicoRepository, usuarioRepository, simulacaoLogin);

        when(simulacaoLogin.loginAtual()).thenReturn("usuario");

        preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.EM_ANALISE);
        guia = new Guia();
        guia.setIdGuia(1L);
        guia.setNumero("202610678");
        guia.setPreGuia(preGuia);
        when(guiaRepository.findByIdComLock(1L)).thenReturn(Optional.of(guia));
    }

    @Test
    void medicoAssinaPrimeiro_registraAssinatura_enaoConcluiAPreGuia() {
        logarComo(usuario(PerfilUsuario.MEDICO, true, "assinaturas/medico.png"));
        when(assinaturaGuiaRepository.buscarComLockPorGuia(1L)).thenReturn(List.of());

        AssinaturaRegistradaResponse resposta = service.assinar(1L);

        assertThat(resposta.assinaturas()).containsExactly(AssinaturaGuiaPapel.MEDICO);
        assertThat(resposta.concluida()).isFalse();
        assertThat(preGuia.getStatus()).isEqualTo(PreGuiaStatus.EM_ANALISE);
        verifyNoInteractions(historicoRepository);

        ArgumentCaptor<AssinaturaGuia> gravada = ArgumentCaptor.forClass(AssinaturaGuia.class);
        verify(assinaturaGuiaRepository).saveAndFlush(gravada.capture());
        assertThat(gravada.getValue().getPapel()).isEqualTo(AssinaturaGuiaPapel.MEDICO);
        assertThat(gravada.getValue().getGuia()).isSameAs(guia);
    }

    @Test
    void chefeAssinaSegundo_concluiAPreGuia_eRegistraHistorico() {
        logarComo(usuario(PerfilUsuario.CHEFE_FUSEX, true, "assinaturas/chefe.png"));
        when(assinaturaGuiaRepository.buscarComLockPorGuia(1L))
                .thenReturn(List.of(assinatura(AssinaturaGuiaPapel.MEDICO)));

        AssinaturaRegistradaResponse resposta = service.assinar(1L);

        assertThat(resposta.assinaturas())
                .containsExactly(AssinaturaGuiaPapel.MEDICO, AssinaturaGuiaPapel.CHEFE);
        assertThat(resposta.concluida()).isTrue();
        assertThat(preGuia.getStatus()).isEqualTo(PreGuiaStatus.CONCLUIDA);

        ArgumentCaptor<PreGuiaHistorico> historico = ArgumentCaptor.forClass(PreGuiaHistorico.class);
        verify(historicoRepository).save(historico.capture());
        assertThat(historico.getValue().getStatus()).isEqualTo(PreGuiaStatus.CONCLUIDA);
        assertThat(historico.getValue().getPreGuia()).isSameAs(preGuia);
    }

    @Test
    void papelJaAssinado_lancaConflito_eNaoGrava() {
        logarComo(usuario(PerfilUsuario.MEDICO, true, "assinaturas/medico.png"));
        when(assinaturaGuiaRepository.buscarComLockPorGuia(1L))
                .thenReturn(List.of(assinatura(AssinaturaGuiaPapel.MEDICO)));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Esta Guia ja foi assinada por um medico do FUSEX");

        verify(assinaturaGuiaRepository, never()).saveAndFlush(any());
    }

    @Test
    void doisMedicosAoMesmoTempo_violacaoDeUnicidadeViraConflito() {
        logarComo(usuario(PerfilUsuario.MEDICO, true, "assinaturas/medico.png"));
        when(assinaturaGuiaRepository.buscarComLockPorGuia(1L)).thenReturn(List.of());
        when(assinaturaGuiaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_assinaturaguia_guia_id_papel_unico"));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOf(ConflitoException.class);
    }

    @Test
    void usuarioSemAssinaturaCadastrada_lancaDadosInvalidos_eNaoGrava() {
        logarComo(usuario(PerfilUsuario.MEDICO, true, null));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Cadastre sua assinatura");

        verify(assinaturaGuiaRepository, never()).saveAndFlush(any());
    }

    @Test
    void assinaturaEmBranco_contaComoNaoCadastrada() {
        logarComo(usuario(PerfilUsuario.MEDICO, true, "   "));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void usuarioInativo_lancaAcessoNegado() {
        logarComo(usuario(PerfilUsuario.MEDICO, false, "assinaturas/medico.png"));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOf(AcessoNegadoException.class);

        verify(assinaturaGuiaRepository, never()).saveAndFlush(any());
    }

    @Test
    void perfilSemPermissao_lancaAcessoNegado() {
        logarComo(usuario(PerfilUsuario.BENEFICIARIO, true, "assinaturas/x.png"));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(guiaRepository);
    }

    @Test
    void guiaInexistente_lancaRecursoNaoEncontrado() {
        logarComo(usuario(PerfilUsuario.MEDICO, true, "assinaturas/medico.png"));
        when(guiaRepository.findByIdComLock(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assinar(9L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void usuarioNaoCadastrado_lancaNaoAutenticado() {
        when(usuarioRepository.findByLogin("usuario")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOf(NaoAutenticadoException.class);
    }

    private void logarComo(Usuario usuario) {
        when(usuarioRepository.findByLogin("usuario")).thenReturn(Optional.of(usuario));
    }

    private static Usuario usuario(PerfilUsuario perfil, boolean ativo, String assinaturaUrl) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        usuario.setAssinaturaUrl(assinaturaUrl);
        return usuario;
    }

    private static AssinaturaGuia assinatura(AssinaturaGuiaPapel papel) {
        AssinaturaGuia assinatura = new AssinaturaGuia();
        assinatura.setPapel(papel);
        return assinatura;
    }
}