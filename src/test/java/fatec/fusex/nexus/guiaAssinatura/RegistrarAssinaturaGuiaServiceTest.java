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

import fatec.fusex.nexus.assinatura.Assinatura;
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
import fatec.fusex.nexus.common.auth.UsuarioLogado;
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

/**
 * Testes da assinatura da Guia (T5-BE3): assinatura de cada papel, conclusao com as duas, segunda assinatura,
 * usuario sem assinatura cadastrada, usuario inativo, perfil errado e motivo de cada conflito (T9-BE1).
 */
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
                historicoRepository, new UsuarioLogado(simulacaoLogin, usuarioRepository));

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
    void medicoJaAssinouAGuia_motivoPeloUsuario() {
        logarComo(usuarioComId(PerfilUsuario.MEDICO, 7L));
        when(assinaturaGuiaRepository.buscarComLockPorGuia(1L))
                .thenReturn(List.of(assinaturaDe(AssinaturaGuiaPapel.MEDICO, 7L)));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOfSatisfying(AssinaturaGuiaConflitoException.class,
                        e -> assertThat(e.getMotivo()).isEqualTo(MotivoFalhaAssinatura.JA_ASSINADA_PELO_USUARIO));

        verify(assinaturaGuiaRepository, never()).saveAndFlush(any());
    }

    @Test
    void medicoAssinadaPorOutroMedico_motivoPorMedico() {
        logarComo(usuarioComId(PerfilUsuario.MEDICO, 7L));
        when(assinaturaGuiaRepository.buscarComLockPorGuia(1L))
                .thenReturn(List.of(assinaturaDe(AssinaturaGuiaPapel.MEDICO, 9L)));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOfSatisfying(AssinaturaGuiaConflitoException.class,
                        e -> assertThat(e.getMotivo()).isEqualTo(MotivoFalhaAssinatura.JA_ASSINADA_POR_MEDICO));

        verify(assinaturaGuiaRepository, never()).saveAndFlush(any());
    }

    @Test
    void chefeJaAssinou_motivoPeloChefe() {
        logarComo(usuarioComId(PerfilUsuario.CHEFE_FUSEX, 3L));
        when(assinaturaGuiaRepository.buscarComLockPorGuia(1L))
                .thenReturn(List.of(assinaturaDe(AssinaturaGuiaPapel.CHEFE, 3L)));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOfSatisfying(AssinaturaGuiaConflitoException.class,
                        e -> assertThat(e.getMotivo()).isEqualTo(MotivoFalhaAssinatura.JA_ASSINADA_PELO_CHEFE));
    }

    @Test
    void preGuiaJaConcluida_motivoConcluida_eNaoGrava() {
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        logarComo(usuarioComId(PerfilUsuario.MEDICO, 7L));
        when(assinaturaGuiaRepository.buscarComLockPorGuia(1L))
                .thenReturn(List.of(assinaturaDe(AssinaturaGuiaPapel.CHEFE, 3L)));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOfSatisfying(AssinaturaGuiaConflitoException.class,
                        e -> assertThat(e.getMotivo()).isEqualTo(MotivoFalhaAssinatura.CONCLUIDA));

        verify(assinaturaGuiaRepository, never()).saveAndFlush(any());
    }

    @Test
    void doisMedicosAoMesmoTempo_violacaoDeUnicidadeViraConflitoComMotivo() {
        logarComo(usuario(PerfilUsuario.MEDICO, true, "assinaturas/medico.png"));
        when(assinaturaGuiaRepository.buscarComLockPorGuia(1L)).thenReturn(List.of());
        when(assinaturaGuiaRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_assinaturaguia_guia_id_papel_unico"));

        assertThatThrownBy(() -> service.assinar(1L))
                .isInstanceOfSatisfying(AssinaturaGuiaConflitoException.class,
                        e -> assertThat(e.getMotivo()).isEqualTo(MotivoFalhaAssinatura.JA_ASSINADA_POR_MEDICO));
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
        if (assinaturaUrl != null) {
            Assinatura assinatura = new Assinatura();
            assinatura.setAssinaturaUrl(assinaturaUrl);
            usuario.setAssinatura(assinatura);
        }
        return usuario;
    }

    private static Usuario usuarioComId(PerfilUsuario perfil, Long id) {
        Usuario usuario = usuario(perfil, true, "assinaturas/" + perfil.name().toLowerCase() + ".png");
        usuario.setIdUsuario(id);
        return usuario;
    }

    private static AssinaturaGuia assinatura(AssinaturaGuiaPapel papel) {
        AssinaturaGuia assinatura = new AssinaturaGuia();
        assinatura.setPapel(papel);
        return assinatura;
    }

    private static AssinaturaGuia assinaturaDe(AssinaturaGuiaPapel papel, Long idUsuario) {
        Usuario autor = new Usuario();
        autor.setIdUsuario(idUsuario);
        AssinaturaGuia assinatura = assinatura(papel);
        assinatura.setUsuario(autor);
        return assinatura;
    }
}