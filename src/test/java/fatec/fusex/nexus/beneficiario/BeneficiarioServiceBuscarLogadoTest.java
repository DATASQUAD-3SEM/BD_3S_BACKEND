package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Testes da leitura dos dados do beneficiario logado (T10-BE1), incluindo perfil errado e assinatura cadastrada.
 */
@ExtendWith(MockitoExtension.class)
class BeneficiarioServiceBuscarLogadoTest {

    private static final String CPF = "52998224725";

    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SimulacaoLogin simulacaoLogin;
    @InjectMocks private BeneficiarioService service;

    @Test
    void buscarLogado_quandoBeneficiario_devolveSeusDados() {
        Usuario usuario = usuario(7L, PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = beneficiario(usuario);

        when(simulacaoLogin.loginAtual()).thenReturn(CPF);
        when(usuarioRepository.findByLogin(CPF)).thenReturn(Optional.of(usuario));
        when(beneficiarioRepository.findById(7L)).thenReturn(Optional.of(beneficiario));

        BeneficiarioResponse resposta = service.buscarLogado();

        assertThat(resposta.cpf()).isEqualTo(CPF);
        assertThat(resposta.nome()).isEqualTo("Antonio Carlos Ferreira");
    }

    @Test
    void buscarLogado_quandoPerfilErrado_lancaAcessoNegadoSemBuscarBeneficiario() {
        Usuario medico = usuario(8L, PerfilUsuario.MEDICO);

        when(simulacaoLogin.loginAtual()).thenReturn(CPF);
        when(usuarioRepository.findByLogin(CPF)).thenReturn(Optional.of(medico));

        assertThatThrownBy(() -> service.buscarLogado())
                .isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(beneficiarioRepository);
    }

    @Test
    void buscarLogado_quandoUsuarioNaoExiste_lancaNaoAutenticado() {
        when(simulacaoLogin.loginAtual()).thenReturn(CPF);
        when(usuarioRepository.findByLogin(CPF)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarLogado())
                .isInstanceOf(NaoAutenticadoException.class);
        verifyNoInteractions(beneficiarioRepository);
    }

    @Test
    void buscarLogado_quandoBeneficiarioNaoTemRegistro_lancaNaoEncontrado() {
        Usuario usuario = usuario(9L, PerfilUsuario.BENEFICIARIO);

        when(simulacaoLogin.loginAtual()).thenReturn(CPF);
        when(usuarioRepository.findByLogin(CPF)).thenReturn(Optional.of(usuario));
        when(beneficiarioRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarLogado())
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Usuario usuario(Long id, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(id);
        usuario.setLogin(CPF);
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }

    private Beneficiario beneficiario(Usuario usuario) {
        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome("Antonio Carlos Ferreira");
        b.setPrecCp("45872213300");
        b.setDataNascimento(LocalDate.of(1968, 5, 10));
        b.setTelefone("12987654321");
        return b;
    }
}