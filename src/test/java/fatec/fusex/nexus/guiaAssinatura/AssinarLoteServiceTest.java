package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.LimiteRequisicoesException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.guiaAssinatura.dto.AssinarLoteResponse;
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
        when(simulacaoLogin.loginAtual()).thenReturn("chefe");
    }

    @Test
    void loteDeUmaGuia_assinaEDevolveAssinadas() {
        logarComo(chefe(true, true));

        AssinarLoteResponse resposta = service.assinar(List.of(1L));

        assertThat(resposta.assinadas()).containsExactly(1L);
        assertThat(resposta.falhas()).isEmpty();
        verify(registrarAssinaturaGuiaService).assinar(1L);
    }

    @Test
    void loteDeVarias_assinaTodasEmOrdemCrescente_semDuplicar() {
        logarComo(chefe(true, true));

        AssinarLoteResponse resposta = service.assinar(List.of(3L, 1L, 2L, 3L));

        assertThat(resposta.assinadas()).containsExactly(1L, 2L, 3L);
        verify(registrarAssinaturaGuiaService).assinar(1L);
        verify(registrarAssinaturaGuiaService).assinar(2L);
        verify(registrarAssinaturaGuiaService).assinar(3L);
    }

    @Test
    void falhasPorGuia_naoCancelamAsOutras_eTraduzemOMotivo() {
        logarComo(chefe(true, true));
        when(registrarAssinaturaGuiaService.assinar(2L))
                .thenThrow(new ConflitoException("Esta Guia ja foi assinada pelo chefe do FUSEx"));
        when(registrarAssinaturaGuiaService.assinar(3L))
                .thenThrow(new RecursoNaoEncontradoException("Guia nao encontrada: id 3"));

        AssinarLoteResponse resposta = service.assinar(List.of(1L, 2L, 3L, 4L));

        assertThat(resposta.assinadas()).containsExactly(1L, 4L);
        assertThat(resposta.falhas()).hasSize(2);
        assertThat(resposta.falhas().get(0).guiaId()).isEqualTo(2L);
        assertThat(resposta.falhas().get(0).motivo()).isEqualTo("ja_assinada_pelo_chefe");
        assertThat(resposta.falhas().get(1).guiaId()).isEqualTo(3L);
        assertThat(resposta.falhas().get(1).motivo()).isEqualTo("guia_nao_encontrada");
    }

    @Test
    void listaNulaOuVazia_devolveRegraDeNegocio_eNaoConsomeLimite() {
        logarComo(chefe(true, true));

        assertThatThrownBy(() -> service.assinar(null)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.assinar(List.of())).isInstanceOf(RegraDeNegocioException.class);

        verifyNoInteractions(registrarAssinaturaGuiaService);
        verifyNoInteractions(limiteLoteAssinatura);
    }

    @Test
    void idNuloOuNaoPositivo_devolveRegraDeNegocio() {
        logarComo(chefe(true, true));

        assertThatThrownBy(() -> service.assinar(Arrays.asList(1L, null)))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.assinar(List.of(0L))).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.assinar(List.of(-4L))).isInstanceOf(RegraDeNegocioException.class);

        verifyNoInteractions(registrarAssinaturaGuiaService);
    }

    @Test
    void cinquentaGuias_saoAceitas_cinquentaEUma_saoRecusadas() {
        logarComo(chefe(true, true));

        List<Long> cinquenta = LongStream.rangeClosed(1, 50).boxed().toList();
        assertThat(service.assinar(cinquenta).assinadas()).hasSize(50);

        List<Long> cinquentaEUma = LongStream.rangeClosed(1, 51).boxed().toList();
        assertThatThrownBy(() -> service.assinar(cinquentaEUma))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Selecione ate 50 guias por vez.");
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
        logarComo(chefe(true, true));
        doThrow(new LimiteRequisicoesException("Muitos lotes"))
                .when(limiteLoteAssinatura).registrar("chefe");

        assertThatThrownBy(() -> service.assinar(List.of(1L)))
                .isInstanceOf(LimiteRequisicoesException.class);

        verify(registrarAssinaturaGuiaService, never()).assinar(anyLong());
    }

    @Test
    void perfilMedico_devolveAcessoNegado_eNaoAssina() {
        Usuario medico = chefe(true, true);
        medico.setPerfil(PerfilUsuario.MEDICO);
        logarComo(medico);

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
        when(usuarioRepository.findByLogin("chefe")).thenReturn(Optional.of(usuario));
    }

    private static Usuario chefe(boolean ativo, boolean comAssinatura) {
        Usuario usuario = new Usuario();
        usuario.setLogin("chefe");
        usuario.setPerfil(PerfilUsuario.CHEFE_FUSEX);
        usuario.setAtivo(ativo);
        if (comAssinatura) {
            Assinatura assinatura = new Assinatura();
            assinatura.setAssinaturaUrl("assinaturas/chefe.png");
            usuario.setAssinatura(assinatura);
        }
        return usuario;
    }
}
