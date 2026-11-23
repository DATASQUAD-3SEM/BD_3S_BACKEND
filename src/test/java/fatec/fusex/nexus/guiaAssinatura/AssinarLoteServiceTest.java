package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.LimiteRequisicoesException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.guiaAssinatura.dto.AssinarLoteResponse;
import fatec.fusex.nexus.guiaAssinatura.dto.FalhaAssinaturaLote;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

class AssinarLoteServiceTest {

    @Mock
    private RegistrarAssinaturaGuiaService registrarAssinaturaGuiaService;

    @Mock
    private LimiteLoteAssinatura limiteLoteAssinatura;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    private AssinarLoteService service;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new AssinarLoteService(registrarAssinaturaGuiaService, limiteLoteAssinatura,
                new UsuarioLogado(simulacaoLogin, usuarioRepository));
    }

    @Test
    void medico_loteDeUmaGuia_assinaEDevolveAssinadas() {
        logarComo(medico(true, true));

        AssinarLoteResponse resposta = service.assinar(List.of(1L));

        assertThat(resposta.assinadas()).containsExactly(1L);
        assertThat(resposta.falhas()).isEmpty();
        verify(registrarAssinaturaGuiaService).assinar(1L);
    }

    @Test
    void medico_loteDeVarias_assinaTodasEmOrdemCrescente_semDuplicar() {
        logarComo(medico(true, true));

        AssinarLoteResponse resposta = service.assinar(List.of(3L, 1L, 2L, 3L));

        assertThat(resposta.assinadas()).containsExactly(1L, 2L, 3L);
        verify(registrarAssinaturaGuiaService).assinar(1L);
        verify(registrarAssinaturaGuiaService).assinar(2L);
        verify(registrarAssinaturaGuiaService).assinar(3L);
    }

    @Test
    void medico_falhasPorGuia_traduzemOMotivoDeCadaCaso_eNaoCancelamAsOutras() {
        logarComo(medico(true, true));
        when(registrarAssinaturaGuiaService.assinar(2L))
                .thenThrow(new AssinaturaGuiaConflitoException(MotivoFalhaAssinatura.JA_ASSINADA_POR_MEDICO,
                        "Esta Guia ja foi assinada por um medico do FUSEX"));
        when(registrarAssinaturaGuiaService.assinar(3L))
                .thenThrow(new AssinaturaGuiaConflitoException(MotivoFalhaAssinatura.JA_ASSINADA_PELO_USUARIO,
                        "Esta Guia ja foi assinada por um medico do FUSEX"));
        when(registrarAssinaturaGuiaService.assinar(4L))
                .thenThrow(new AssinaturaGuiaConflitoException(MotivoFalhaAssinatura.CONCLUIDA,
                        "Esta Guia ja esta concluida"));
        when(registrarAssinaturaGuiaService.assinar(5L))
                .thenThrow(new RecursoNaoEncontradoException("Guia nao encontrada: id 5"));

        AssinarLoteResponse resposta = service.assinar(List.of(1L, 2L, 3L, 4L, 5L));

        assertThat(resposta.assinadas()).containsExactly(1L);
        assertThat(resposta.falhas())
                .extracting(FalhaAssinaturaLote::guiaId, FalhaAssinaturaLote::motivo)
                .containsExactly(
                        tuple(2L, "ja_assinada_por_medico"),
                        tuple(3L, "ja_assinada_pelo_usuario"),
                        tuple(4L, "concluida"),
                        tuple(5L, "guia_nao_encontrada"));
    }

    @Test
    void chefe_loteDeUmaGuia_assinaEDevolveAssinadas() {
        logarComo(chefe(true, true));

        AssinarLoteResponse resposta = service.assinar(List.of(1L));

        assertThat(resposta.assinadas()).containsExactly(1L);
        assertThat(resposta.falhas()).isEmpty();
        verify(registrarAssinaturaGuiaService).assinar(1L);
    }

    @Test
    void chefe_falhasPorGuia_traduzemOMotivoDoChefe_eNaoCancelamAsOutras() {
        logarComo(chefe(true, true));
        when(registrarAssinaturaGuiaService.assinar(2L))
                .thenThrow(new AssinaturaGuiaConflitoException(MotivoFalhaAssinatura.JA_ASSINADA_PELO_CHEFE,
                        "Esta Guia ja foi assinada pelo chefe do FUSEx"));
        when(registrarAssinaturaGuiaService.assinar(3L))
                .thenThrow(new RecursoNaoEncontradoException("Guia nao encontrada: id 3"));

        AssinarLoteResponse resposta = service.assinar(List.of(1L, 2L, 3L, 4L));

        assertThat(resposta.assinadas()).containsExactly(1L, 4L);
        assertThat(resposta.falhas())
                .extracting(FalhaAssinaturaLote::guiaId, FalhaAssinaturaLote::motivo)
                .containsExactly(
                        tuple(2L, "ja_assinada_pelo_chefe"),
                        tuple(3L, "guia_nao_encontrada"));
    }

    @Test
    void listaNulaOuVazia_devolveRegraDeNegocio_eNaoConsomeLimite() {
        logarComo(medico(true, true));

        assertThatThrownBy(() -> service.assinar(null)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.assinar(List.of())).isInstanceOf(RegraDeNegocioException.class);

        verifyNoInteractions(registrarAssinaturaGuiaService);
        verifyNoInteractions(limiteLoteAssinatura);
    }

    @Test
    void idNuloOuNaoPositivo_devolveRegraDeNegocio() {
        logarComo(medico(true, true));

        assertThatThrownBy(() -> service.assinar(Arrays.asList(1L, null)))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.assinar(List.of(0L))).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.assinar(List.of(-4L))).isInstanceOf(RegraDeNegocioException.class);

        verifyNoInteractions(registrarAssinaturaGuiaService);
    }

    @Test
    void cinquentaGuias_saoAceitas_cinquentaEUma_saoRecusadas() {
        logarComo(medico(true, true));

        List<Long> cinquenta = LongStream.rangeClosed(1, 50).boxed().toList();
        assertThat(service.assinar(cinquenta).assinadas()).hasSize(50);

        List<Long> cinquentaEUma = LongStream.rangeClosed(1, 51).boxed().toList();
        assertThatThrownBy(() -> service.assinar(cinquentaEUma))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Selecione ate 50 guias por vez.");
    }

    @Test
    void medicoSemAssinaturaCadastrada_devolveDadosInvalidos_eNaoAssina() {
        logarComo(medico(true, false));

        assertThatThrownBy(() -> service.assinar(List.of(1L)))
                .isInstanceOf(DadosInvalidosException.class);

        verifyNoInteractions(registrarAssinaturaGuiaService);
    }

    @Test
    void chefeSemAssinaturaCadastrada_devolveDadosInvalidos_eNaoAssina() {
        logarComo(chefe(true, false));

        assertThatThrownBy(() -> service.assinar(List.of(1L)))
                .isInstanceOf(DadosInvalidosException.class);

        verifyNoInteractions(registrarAssinaturaGuiaService);
    }

    @Test
    void limiteExcedido_propagaComoLimite_eNaoAssina() {
        logarComo(medico(true, true));
        doThrow(new LimiteRequisicoesException("Muitos lotes"))
                .when(limiteLoteAssinatura).registrar("medico");

        assertThatThrownBy(() -> service.assinar(List.of(1L)))
                .isInstanceOf(LimiteRequisicoesException.class);

        verify(registrarAssinaturaGuiaService, never()).assinar(anyLong());
    }

    @Test
    void perfilSemPermissao_devolveAcessoNegado_eNaoAssina() {
        logarComo(usuario(PerfilUsuario.BENEFICIARIO, "beneficiario", true, true));

        assertThatThrownBy(() -> service.assinar(List.of(1L)))
                .isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(registrarAssinaturaGuiaService);
    }

    @Test
    void medicoInativo_devolveAcessoNegado() {
        logarComo(medico(false, true));

        assertThatThrownBy(() -> service.assinar(List.of(1L)))
                .isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(registrarAssinaturaGuiaService);
    }

    @Test
    void chefeInativo_devolveAcessoNegado() {
        logarComo(chefe(false, true));

        assertThatThrownBy(() -> service.assinar(List.of(1L)))
                .isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(registrarAssinaturaGuiaService);
    }

    private void logarComo(Usuario usuario) {
        when(simulacaoLogin.loginAtual()).thenReturn(usuario.getLogin());
        when(usuarioRepository.findByLogin(usuario.getLogin())).thenReturn(Optional.of(usuario));
    }

    private static Usuario medico(boolean ativo, boolean comAssinatura) {
        return usuario(PerfilUsuario.MEDICO, "medico", ativo, comAssinatura);
    }

    private static Usuario chefe(boolean ativo, boolean comAssinatura) {
        return usuario(PerfilUsuario.CHEFE_FUSEX, "chefe", ativo, comAssinatura);
    }

    private static Usuario usuario(PerfilUsuario perfil, String login, boolean ativo, boolean comAssinatura) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        if (comAssinatura) {
            Assinatura assinatura = new Assinatura();
            assinatura.setAssinaturaUrl("assinaturas/" + login + ".png");
            usuario.setAssinatura(assinatura);
        }
        return usuario;
    }
}