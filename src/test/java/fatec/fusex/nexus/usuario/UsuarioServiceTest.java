package fatec.fusex.nexus.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.MuitasTentativasException;
import fatec.fusex.nexus.usuario.dto.AlterarSenhaRequest;

/**
 * Testes da troca da propria senha (T13-BE1): senha atual errada, nova igual a atual, confirmacao diferente,
 * senha curta, limite de tentativas e usuario inativo.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String LOGIN = "usuario-teste";
    private static final String HASH_ATUAL = "hash-atual";
    private static final String HASH_NOVO = "hash-novo";
    private static final String SENHA_ATUAL = "SenhaAtual123";
    private static final String SENHA_NOVA = "SenhaNova456";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TentativasSenhaService tentativasSenhaService;
    private UsuarioService usuarioService;
    private Usuario usuario;

    @BeforeEach
    void configurar() {
        tentativasSenhaService = new TentativasSenhaService();

        usuarioService = new UsuarioService(
                usuarioRepository,
                simulacaoLogin,
                passwordEncoder,
                tentativasSenhaService);

        usuario = new Usuario();
        usuario.setIdUsuario(1L);
        usuario.setLogin(LOGIN);
        usuario.setSenhaHash(HASH_ATUAL);
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuario.setAtivo(true);

        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(usuario));
    }

    @Test
    void alterarSenha_quandoDadosValidos_salvaNovoHash() {
        prepararSenhaAtualCorreta();

        usuarioService.alterarSenha(
                new AlterarSenhaRequest(
                        SENHA_ATUAL, SENHA_NOVA, SENHA_NOVA));

        assertThat(usuario.getSenhaHash()).isEqualTo(HASH_NOVO);
        assertThat(usuario.getSenhaHash())
                .isNotEqualTo(SENHA_NOVA);

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void alterarSenha_quandoUsuarioMedico_permiteAlteracao() {
        usuario.setPerfil(PerfilUsuario.MEDICO);
        prepararSenhaAtualCorreta();

        usuarioService.alterarSenha(
                new AlterarSenhaRequest(
                        SENHA_ATUAL, SENHA_NOVA, SENHA_NOVA));

        verify(usuarioRepository).save(usuario);
    }

    @Test
    void alterarSenha_quandoSenhaAtualIncorreta_devolveErroNoCampoCorreto() {
        when(passwordEncoder.matches(SENHA_ATUAL, HASH_ATUAL))
                .thenReturn(false);

        Throwable erro = catchThrowable(() ->
                usuarioService.alterarSenha(
                        new AlterarSenhaRequest(
                                SENHA_ATUAL, SENHA_NOVA, SENHA_NOVA)));

        assertThat(erro).isInstanceOf(DadosInvalidosException.class);
        assertThat(((DadosInvalidosException) erro).getCampo())
                .isEqualTo("senhaAtual");
    }

    @Test
    void alterarSenha_quandoNovaSenhaTemMenosDeOitoCaracteres_rejeita() {
        Throwable erro = catchThrowable(() ->
                usuarioService.alterarSenha(
                        new AlterarSenhaRequest(
                                SENHA_ATUAL, "1234567", "1234567")));

        assertThat(erro).isInstanceOf(DadosInvalidosException.class);
        assertThat(((DadosInvalidosException) erro).getCampo())
                .isEqualTo("novaSenha");
    }

    @Test
    void alterarSenha_quandoConfirmacaoDiferente_rejeita() {
        Throwable erro = catchThrowable(() ->
                usuarioService.alterarSenha(
                        new AlterarSenhaRequest(
                                SENHA_ATUAL, SENHA_NOVA, "SenhaDiferente789")));

        assertThat(erro).isInstanceOf(DadosInvalidosException.class);
        assertThat(((DadosInvalidosException) erro).getCampo())
                .isEqualTo("confirmacao");
    }

    @Test
    void alterarSenha_quandoNovaSenhaIgualAtual_rejeita() {
        when(passwordEncoder.matches(SENHA_ATUAL, HASH_ATUAL))
                .thenReturn(true);

        Throwable erro = catchThrowable(() ->
                usuarioService.alterarSenha(
                        new AlterarSenhaRequest(
                                SENHA_ATUAL, SENHA_ATUAL, SENHA_ATUAL)));

        assertThat(erro).isInstanceOf(DadosInvalidosException.class);
        assertThat(((DadosInvalidosException) erro).getCampo())
                .isEqualTo("novaSenha");
    }

    @Test
    void alterarSenha_quandoUsuarioInativo_devolveAcessoNegado() {
        usuario.setAtivo(false);

        Throwable erro = catchThrowable(() ->
                usuarioService.alterarSenha(
                        new AlterarSenhaRequest(
                                SENHA_ATUAL, SENHA_NOVA, SENHA_NOVA)));

        assertThat(erro).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void alterarSenha_quandoPerfilChefeFusex_devolveAcessoNegado() {
        usuario.setPerfil(PerfilUsuario.CHEFE_FUSEX);

        Throwable erro = catchThrowable(() ->
                usuarioService.alterarSenha(
                        new AlterarSenhaRequest(
                                SENHA_ATUAL, SENHA_NOVA, SENHA_NOVA)));

        assertThat(erro).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void alterarSenha_quandoCincoSenhasAtuaisIncorretas_bloqueiaTentativaSeguinte() {
        when(passwordEncoder.matches(SENHA_ATUAL, HASH_ATUAL))
                .thenReturn(false);

        for (int i = 0; i < 5; i++) {
            Throwable erro = catchThrowable(() ->
                    usuarioService.alterarSenha(
                            new AlterarSenhaRequest(
                                    SENHA_ATUAL, SENHA_NOVA, SENHA_NOVA)));

            assertThat(erro).isInstanceOf(DadosInvalidosException.class);
        }

        Throwable erro = catchThrowable(() ->
                usuarioService.alterarSenha(
                        new AlterarSenhaRequest(
                                SENHA_ATUAL, SENHA_NOVA, SENHA_NOVA)));

        assertThat(erro).isInstanceOf(MuitasTentativasException.class);
    }

    private void prepararSenhaAtualCorreta() {
        when(passwordEncoder.matches(SENHA_ATUAL, HASH_ATUAL))
                .thenReturn(true);
        when(passwordEncoder.matches(SENHA_NOVA, HASH_ATUAL))
                .thenReturn(false);
        when(passwordEncoder.encode(SENHA_NOVA))
                .thenReturn(HASH_NOVO);
    }
}