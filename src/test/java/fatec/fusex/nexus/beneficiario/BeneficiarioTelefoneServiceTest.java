package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

@ExtendWith(MockitoExtension.class)
class BeneficiarioTelefoneServiceTest {

    private static final String LOGIN = "52998224725";
    private static final Long ID = 7L;

    @Mock
    private BeneficiarioRepository beneficiarioRepository;

    @Mock
    private fatec.fusex.nexus.usuario.UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @InjectMocks
    private BeneficiarioService service;

    @Test
    void atualizarTelefone_comTelefoneValido_salvaSomenteOTelefone() {
        Usuario usuario = usuario(PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = beneficiario(usuario);

        autenticarComo(usuario, beneficiario);

        String resultado = service.atualizarTelefoneLogado("(12) 98765-4321");

        assertThat(resultado).isEqualTo("12987654321");
        assertThat(beneficiario.getTelefone()).isEqualTo("12987654321");

        assertThat(beneficiario.getNome()).isEqualTo("Antonio Carlos Ferreira");
        assertThat(beneficiario.getPrecCp()).isEqualTo("45872213300");
        assertThat(beneficiario.getDataNascimento())
                .isEqualTo(LocalDate.of(1968, 3, 15));

        verify(beneficiarioRepository).save(beneficiario);
    }

    @Test
    void atualizarTelefone_comFormatoInvalido_lanca422ESemSalvar() {
        Usuario usuario = usuario(PerfilUsuario.BENEFICIARIO);
        Beneficiario beneficiario = beneficiario(usuario);

        autenticarComo(usuario, beneficiario);

        assertThatThrownBy(() -> service.atualizarTelefoneLogado("123"))
                .isInstanceOfSatisfying(
                        DadosInvalidosException.class,
                        ex -> assertThat(ex.getCampo()).isEqualTo("telefone")
                );

        assertThat(beneficiario.getTelefone()).isEqualTo("11999999999");
        verify(beneficiarioRepository, never()).save(any(Beneficiario.class));
    }

    @Test
    void atualizarTelefone_comPerfilErrado_lanca403() {
        Usuario medico = usuario(PerfilUsuario.MEDICO);

        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(medico));

        assertThatThrownBy(
                () -> service.atualizarTelefoneLogado("12987654321")
        ).isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(beneficiarioRepository);
    }

    private void autenticarComo(
            Usuario usuario,
            Beneficiario beneficiario) {

        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(usuario));
        when(beneficiarioRepository.findById(ID))
                .thenReturn(Optional.of(beneficiario));
    }

    private Usuario usuario(PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(ID);
        usuario.setLogin(LOGIN);
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        return usuario;
    }

    private Beneficiario beneficiario(Usuario usuario) {
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setUsuarioId(ID);
        beneficiario.setUsuario(usuario);
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");
        beneficiario.setDataNascimento(LocalDate.of(1968, 3, 15));
        beneficiario.setTelefone("11999999999");
        return beneficiario;
    }
}