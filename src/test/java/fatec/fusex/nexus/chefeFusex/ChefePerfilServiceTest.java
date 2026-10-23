package fatec.fusex.nexus.chefeFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fatec.fusex.nexus.chefeFusex.dto.ChefePerfilResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class ChefePerfilServiceTest {

    private static final Long USUARIO_ID = 10L;
    private static final String LOGIN = "chefe-teste";

    @Mock
    private ChefeFusexRepository chefeFusexRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    private ChefePerfilService service;

    @BeforeEach
    void configurar() {
        service = new ChefePerfilService(
                chefeFusexRepository,
                usuarioRepository,
                simulacaoLogin);
    }

    @Test
    void buscarPerfilLogado_quandoChefeCadastrado_retornaDados() {
        Usuario usuario = usuarioChefe();
        usuario.setAssinaturaUrl("assinaturas/chefe.png");

        ChefeFusex chefe = chefe(usuario);
        prepararUsuario(usuario);

        when(chefeFusexRepository.findById(USUARIO_ID))
                .thenReturn(Optional.of(chefe));

        ChefePerfilResponse resposta = service.buscarPerfilLogado();

        assertThat(resposta.nome()).isEqualTo("Maria Oliveira");
        assertThat(resposta.patente()).isEqualTo("Major");
        assertThat(resposta.login()).isEqualTo(LOGIN);
        assertThat(resposta.assinaturaCadastrada()).isTrue();
    }

    @Test
    void buscarPerfilLogado_quandoNaoTemAssinatura_retornaFalse() {
        Usuario usuario = usuarioChefe();
        ChefeFusex chefe = chefe(usuario);

        prepararUsuario(usuario);

        when(chefeFusexRepository.findById(USUARIO_ID))
                .thenReturn(Optional.of(chefe));

        ChefePerfilResponse resposta = service.buscarPerfilLogado();

        assertThat(resposta.assinaturaCadastrada()).isFalse();
    }

    @Test
    void buscarPerfilLogado_quandoPerfilErrado_retornaAcessoNegado() {
        Usuario usuario = usuarioChefe();
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);

        prepararUsuario(usuario);

        assertThatThrownBy(() -> service.buscarPerfilLogado())
                .isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(chefeFusexRepository);
    }

    @Test
    void buscarPerfilLogado_quandoUsuarioNaoExiste_retornaNaoAutenticado() {
        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPerfilLogado())
                .isInstanceOf(NaoAutenticadoException.class);

        verifyNoInteractions(chefeFusexRepository);
    }

    @Test
    void buscarPerfilLogado_quandoCadastroDoChefeNaoExiste_retorna404() {
        Usuario usuario = usuarioChefe();
        prepararUsuario(usuario);

        when(chefeFusexRepository.findById(USUARIO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPerfilLogado())
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private void prepararUsuario(Usuario usuario) {
        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(usuario));
    }

    private Usuario usuarioChefe() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(USUARIO_ID);
        usuario.setLogin(LOGIN);
        usuario.setPerfil(PerfilUsuario.CHEFE_FUSEX);
        usuario.setAtivo(true);
        return usuario;
    }

    private ChefeFusex chefe(Usuario usuario) {
        ChefeFusex chefe = new ChefeFusex();
        chefe.setUsuarioId(USUARIO_ID);
        chefe.setUsuario(usuario);
        chefe.setNome("Maria Oliveira");
        chefe.setPatente("Major");
        return chefe;
    }
}