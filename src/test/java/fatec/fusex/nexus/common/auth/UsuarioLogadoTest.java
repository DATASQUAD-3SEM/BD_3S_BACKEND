package fatec.fusex.nexus.common.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** Identificacao do usuario logado e checagem de perfil compartilhada pelos servicos. */
class UsuarioLogadoTest {

    private SimulacaoLogin simulacaoLogin;
    private UsuarioRepository usuarioRepository;
    private UsuarioLogado usuarioLogado;

    @BeforeEach
    void dados() {
        simulacaoLogin = mock(SimulacaoLogin.class);
        usuarioRepository = mock(UsuarioRepository.class);
        usuarioLogado = new UsuarioLogado(simulacaoLogin, usuarioRepository);
        when(simulacaoLogin.loginAtual()).thenReturn("usuario");
    }

    @Test
    void atual_loginSemConta_lanca401() {
        when(usuarioRepository.findByLogin("usuario")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioLogado.atual()).isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void exigirPerfil_perfilCorreto_devolveUsuario() {
        Usuario usuario = usuario(PerfilUsuario.SETOR_GUIAS, true);
        when(usuarioRepository.findByLogin("usuario")).thenReturn(Optional.of(usuario));

        assertThat(usuarioLogado.exigirPerfil(PerfilUsuario.SETOR_GUIAS)).isSameAs(usuario);
    }

    @Test
    void exigirPerfil_aceitaQualquerUmDosPerfisInformados() {
        when(usuarioRepository.findByLogin("usuario")).thenReturn(Optional.of(usuario(PerfilUsuario.CHEFE_FUSEX, true)));

        assertThat(usuarioLogado.exigirPerfil(PerfilUsuario.MEDICO, PerfilUsuario.CHEFE_FUSEX)).isNotNull();
    }

    @Test
    void exigirPerfil_perfilErrado_lanca403() {
        when(usuarioRepository.findByLogin("usuario")).thenReturn(Optional.of(usuario(PerfilUsuario.RECEPCAO, true)));

        assertThatThrownBy(() -> usuarioLogado.exigirPerfil(PerfilUsuario.BENEFICIARIO))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessage("Acesso negado: este endpoint e exclusivo do perfil BENEFICIARIO");
    }

    @Test
    void exigirPerfilAtivo_contaDesativada_lanca403() {
        when(usuarioRepository.findByLogin("usuario")).thenReturn(Optional.of(usuario(PerfilUsuario.CHEFE_FUSEX, false)));

        assertThatThrownBy(() -> usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX))
                .isInstanceOf(AcessoNegadoException.class);
    }

    private static Usuario usuario(PerfilUsuario perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setLogin("usuario");
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
