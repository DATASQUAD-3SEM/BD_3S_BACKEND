package fatec.fusex.nexus.chefeFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.chefeFusex.dto.AtualizarChefeRequest;
import fatec.fusex.nexus.chefeFusex.dto.ChefePerfilResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Testes da atualizacao da conta do Chefe (T12-BE2): dados validos, login com espacos, login repetido e nova senha.
 */
@ExtendWith(MockitoExtension.class)
class ChefeAtualizacaoServiceTest {

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ChefeFusexRepository chefeFusexRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ChefePerfilService chefePerfilService;

    private Usuario usuario;
    private ChefeFusex chefe;

    @BeforeEach
    void prepararDados() {
        usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setLogin("chefe-antigo");
        usuario.setSenhaHash("hash-antigo");
        usuario.setPerfil(PerfilUsuario.CHEFE_FUSEX);
        usuario.setAtivo(false);
        usuario.setAssinatura(assinatura("assinaturas/chefe.png"));

        chefe = new ChefeFusex();
        chefe.setUsuarioId(1L);
        chefe.setUsuario(usuario);
        chefe.setNome("Nome Antigo");
        chefe.setPatente("Capitão");

        when(simulacaoLogin.loginAtual()).thenReturn("chefe-antigo");
        when(usuarioRepository.findByLogin("chefe-antigo"))
                .thenReturn(Optional.of(usuario));
    }

    private void prepararChefeExistente() {
        when(chefeFusexRepository.findById(1L))
                .thenReturn(Optional.of(chefe));
    }

    private AtualizarChefeRequest requisicao(
            String nome,
            String patente,
            String login,
            String novaSenha
    ) {
        return new AtualizarChefeRequest(
                nome,
                patente,
                login,
                novaSenha
        );
    }

    @Test
    void atualizarPerfil_quandoDadosValidos_atualizaDadosEPreservaSenhaVazia() {
        prepararChefeExistente();

        when(usuarioRepository.findByLogin("chefe-novo"))
                .thenReturn(Optional.empty());

        ChefePerfilResponse resposta =
                chefePerfilService.atualizarPerfil(
                        requisicao(
                                "Nome Novo",
                                "Major",
                                "chefe-novo",
                                ""
                        )
                );

        assertThat(resposta.nome()).isEqualTo("Nome Novo");
        assertThat(resposta.patente()).isEqualTo("Major");
        assertThat(resposta.login()).isEqualTo("chefe-novo");
        assertThat(resposta.assinaturaCadastrada()).isTrue();

        assertThat(usuario.getSenhaHash()).isEqualTo("hash-antigo");
        assertThat(usuario.getPerfil()).isEqualTo(PerfilUsuario.CHEFE_FUSEX);
        assertThat(usuario.getAtivo()).isFalse();

        verify(passwordEncoder, never()).encode(anyString());
        verify(chefeFusexRepository).save(chefe);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void atualizarPerfil_quandoLoginTemEspacos_lancaDadosInvalidosNoCampoLogin() {
        prepararChefeExistente();

        assertThatThrownBy(() ->
                chefePerfilService.atualizarPerfil(
                        requisicao(
                                "Nome Novo",
                                "Major",
                                "chefe novo",
                                ""
                        )
                )
        ).isInstanceOf(DadosInvalidosException.class)
                .satisfies(erro -> assertThat(
                        ((DadosInvalidosException) erro).getCampo()).isEqualTo("login"));

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(chefeFusexRepository, never()).save(any(ChefeFusex.class));
    }

    @Test
    void atualizarPerfil_quandoLoginPertenceAOutroUsuario_lancaConflito() {
        prepararChefeExistente();

        Usuario outroUsuario = new Usuario();
        outroUsuario.setIdUsuario(2L);
        outroUsuario.setLogin("chefe-novo");

        when(usuarioRepository.findByLogin("chefe-novo"))
                .thenReturn(Optional.of(outroUsuario));

        assertThatThrownBy(() ->
                chefePerfilService.atualizarPerfil(
                        requisicao(
                                "Nome Novo",
                                "Major",
                                "chefe-novo",
                                ""
                        )
                )
        ).isInstanceOf(ConflitoException.class);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(chefeFusexRepository, never()).save(any(ChefeFusex.class));
    }

    @Test
    void atualizarPerfil_quandoNovaSenhaValidaArmazenaHash() {
        prepararChefeExistente();

        when(usuarioRepository.findByLogin("chefe-novo"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("SenhaNova123"))
                .thenReturn("novo-hash-bcrypt");

        chefePerfilService.atualizarPerfil(
                requisicao(
                        "Nome Novo",
                        "Major",
                        "chefe-novo",
                        "SenhaNova123"
                )
        );

        assertThat(usuario.getSenhaHash())
                .isEqualTo("novo-hash-bcrypt");

        verify(passwordEncoder).encode("SenhaNova123");
    }

    @Test
    void atualizarPerfil_quandoNovaSenhaCurta_lancaErro() {
        prepararChefeExistente();

        assertThatThrownBy(() ->
                chefePerfilService.atualizarPerfil(
                        requisicao(
                                "Nome Novo",
                                "Major",
                                "chefe-antigo",
                                "1234567"
                        )
                )
        ).isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("8 caracteres");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void atualizarPerfil_quandoPerfilNaoEhChefe_lancaAcessoNegado() {
        usuario.setPerfil(PerfilUsuario.MEDICO);

        assertThatThrownBy(() ->
                chefePerfilService.atualizarPerfil(
                        requisicao(
                                "Nome Novo",
                                "Major",
                                "chefe-novo",
                                ""
                        )
                )
        ).isInstanceOf(AcessoNegadoException.class);

        verify(chefeFusexRepository, never()).save(any(ChefeFusex.class));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    private static Assinatura assinatura(String url) {
        Assinatura assinatura = new Assinatura();
        assinatura.setAssinaturaUrl(url);
        return assinatura;
    }
}