package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Testes da busca de beneficiario ativo por Prec CP (T2-BE1): com e sem mascara, inexistente e inativo.
 */
@ExtendWith(MockitoExtension.class)
class BeneficiarioServiceTest {

    private static final String PREC_CP = "45872213300";

    @Mock
    private BeneficiarioRepository beneficiarioRepository;

    @InjectMocks
    private BeneficiarioService beneficiarioService;

    private static Beneficiario beneficiario(boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setLogin("52998224725");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuario.setAtivo(ativo);

        Beneficiario b = new Beneficiario();
        b.setUsuarioId(7L);
        b.setUsuario(usuario);
        b.setNome("Antonio Carlos Ferreira");
        b.setPrecCp(PREC_CP);
        b.setDataNascimento(LocalDate.of(1968, 3, 15));
        b.setTelefone("12987654321");
        return b;
    }

    @Test
    void buscarAtivoPorPrecCp_semMascara_devolveBeneficiario() {
        when(beneficiarioRepository.findWithUsuarioByPrecCp(PREC_CP)).thenReturn(Optional.of(beneficiario(true)));

        Beneficiario resultado = beneficiarioService.buscarAtivoPorPrecCp("45872213300");

        assertThat(resultado.getNome()).isEqualTo("Antonio Carlos Ferreira");
    }

    @Test
    void buscarAtivoPorPrecCp_comMascara_buscaPeloValorNormalizado() {
        when(beneficiarioRepository.findWithUsuarioByPrecCp(PREC_CP)).thenReturn(Optional.of(beneficiario(true)));

        Beneficiario resultado = beneficiarioService.buscarAtivoPorPrecCp("458.722.133-00");

        assertThat(resultado.getPrecCp()).isEqualTo(PREC_CP);
        verify(beneficiarioRepository).findWithUsuarioByPrecCp(PREC_CP);
    }

    @Test
    void buscarAtivoPorPrecCp_quandoNaoExiste_lancaNaoEncontrado() {
        when(beneficiarioRepository.findWithUsuarioByPrecCp(PREC_CP)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beneficiarioService.buscarAtivoPorPrecCp(PREC_CP))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarAtivoPorPrecCp_quandoInativo_lancaNaoEncontrado() {
        when(beneficiarioRepository.findWithUsuarioByPrecCp(PREC_CP)).thenReturn(Optional.of(beneficiario(false)));

        assertThatThrownBy(() -> beneficiarioService.buscarAtivoPorPrecCp(PREC_CP))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarAtivoPorPrecCp_inativoEInexistente_daoAMesmaMensagem() {
        when(beneficiarioRepository.findWithUsuarioByPrecCp(PREC_CP))
                .thenReturn(Optional.of(beneficiario(false)))
                .thenReturn(Optional.empty());

        String mensagemInativo = capturarMensagem();
        String mensagemInexistente = capturarMensagem();

        assertThat(mensagemInativo).isEqualTo(mensagemInexistente);
    }

    @Test
    void buscarAtivoPorPrecCp_quandoFormatoInvalido_lancaRegraDeNegocioSemIrAoBanco() {
        assertThatThrownBy(() -> beneficiarioService.buscarAtivoPorPrecCp("123"))
                .isInstanceOf(RegraDeNegocioException.class);

        verifyNoInteractions(beneficiarioRepository);
    }

    @Test
    void buscarAtivoPorPrecCp_quandoNulo_lancaRegraDeNegocioSemIrAoBanco() {
        assertThatThrownBy(() -> beneficiarioService.buscarAtivoPorPrecCp(null))
                .isInstanceOf(RegraDeNegocioException.class);

        verifyNoInteractions(beneficiarioRepository);
    }

    private String capturarMensagem() {
        try {
            beneficiarioService.buscarAtivoPorPrecCp(PREC_CP);
            throw new AssertionError("era para lancar excecao");
        } catch (RecursoNaoEncontradoException e) {
            return e.getMessage();
        }
    }
}
