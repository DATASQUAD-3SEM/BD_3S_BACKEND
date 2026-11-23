package fatec.fusex.nexus.ocs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

class ContaOcsServiceTest {

    private final ContaOcsRepository repository = mock(ContaOcsRepository.class);
    private final UsuarioLogado usuarioLogado = mock(UsuarioLogado.class);
    private final ContaOcsService service = new ContaOcsService(repository, usuarioLogado);

    private Usuario usuarioOcs() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setLogin("ocs.teste");
        usuario.setPerfil(PerfilUsuario.OCS);
        return usuario;
    }

    @Test
    void devolveAOcsVinculadaAConta() {
        Ocs ocs = new Ocs();
        ocs.setIdOcs(3L);
        ContaOcs conta = new ContaOcs();
        conta.setOcs(ocs);
        when(usuarioLogado.exigirPerfil(PerfilUsuario.OCS)).thenReturn(usuarioOcs());
        when(repository.findById(7L)).thenReturn(Optional.of(conta));

        assertThat(service.ocsDaContaLogada().getIdOcs()).isEqualTo(3L);
    }

    @Test
    void contaSemVinculoRecebe403() {
        when(usuarioLogado.exigirPerfil(PerfilUsuario.OCS)).thenReturn(usuarioOcs());
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ocsDaContaLogada())
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void perfilDiferenteDeOcsRecebe403() {
        when(usuarioLogado.exigirPerfil(any())).thenThrow(new AcessoNegadoException("negado"));

        assertThatThrownBy(() -> service.ocsDaContaLogada())
                .isInstanceOf(AcessoNegadoException.class);
    }
}
