package fatec.fusex.nexus.dependenteLeitura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.dependente.ResponsavelExterno;
import fatec.fusex.nexus.dependente.ResponsavelExternoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/** T31-BE5: dados pessoais do responsavel legal externo para Minhas informacoes. */
class ResponsavelMeServiceTest {

    @Test
    void devolveNomeCpfESeTemAssinatura() {
        UsuarioLogado logado = mock(UsuarioLogado.class);
        ResponsavelExternoRepository repo = mock(ResponsavelExternoRepository.class);
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(41L);
        usuario.setLogin("41282945067");
        usuario.setPerfil(PerfilUsuario.RESPONSAVEL_EXTERNO);
        when(logado.exigirPerfilAtivo(PerfilUsuario.RESPONSAVEL_EXTERNO)).thenReturn(usuario);
        ResponsavelExterno helena = new ResponsavelExterno();
        helena.setUsuarioId(41L);
        helena.setNome("Helena Costa");
        when(repo.findById(41L)).thenReturn(Optional.of(helena));
        ResponsavelMeService service = new ResponsavelMeService(logado, repo);

        assertThat(service.meusDados())
                .isEqualTo(new ResponsavelMeService.ResponsavelMeResponse("Helena Costa", "41282945067", false));

        usuario.setAssinatura(new Assinatura());
        assertThat(service.meusDados().temAssinatura()).isTrue();
    }
}
