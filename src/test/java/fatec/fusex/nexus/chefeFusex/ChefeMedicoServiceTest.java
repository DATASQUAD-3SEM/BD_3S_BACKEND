package fatec.fusex.nexus.chefeFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChefeMedicoServiceTest {

    @Mock private MedicoFusexRepository medicoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SimulacaoLogin simulacaoLogin;

    private ChefeMedicoService service;

    private Usuario chefe;

    @BeforeEach
    void dados() {
        service = new ChefeMedicoService(medicoRepository, new UsuarioLogado(simulacaoLogin, usuarioRepository));

        chefe = new Usuario();
        chefe.setLogin("chefe.fusex");
        chefe.setPerfil(PerfilUsuario.CHEFE_FUSEX);
        chefe.setAtivo(true);

        when(simulacaoLogin.loginAtual()).thenReturn("chefe.fusex");
        when(usuarioRepository.findByLogin("chefe.fusex")).thenReturn(Optional.of(chefe));
        when(medicoRepository.buscarParaChefe(any(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void listar_semParametros_usaAtivosOrdemPorNomeEPagina0() {
        service.listar(null, null, 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(medicoRepository).buscarParaChefe(eq(true), eq(""), eq(""), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("nome").isAscending()).isTrue();
    }

    @Test
    void listar_situacoes_viramOFiltroDeAtivo() {
        service.listar(null, "INATIVOS", 0, 20);
        verify(medicoRepository).buscarParaChefe(eq(false), anyString(), anyString(), any(Pageable.class));

        service.listar(null, " todos ", 0, 20);
        verify(medicoRepository).buscarParaChefe(isNull(), anyString(), anyString(), any(Pageable.class));
    }

    @Test
    void listar_situacaoInvalida_lanca400() {
        assertThatThrownBy(() -> service.listar(null, "excluidos", 0, 20))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("situacao");
    }

    @Test
    void listar_paginacaoInvalida_lanca400() {
        assertThatThrownBy(() -> service.listar(null, null, -1, 20)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.listar(null, null, 0, 0)).isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.listar(null, null, 0, 101)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    void listar_buscaComMascara_comparaSoOsDigitos() {
        service.listar(" 111.444.777-35 ", null, 0, 20);

        verify(medicoRepository).buscarParaChefe(eq(true), eq("111.444.777-35"), eq("11144477735"), any(Pageable.class));
    }

    @Test
    void listar_buscaPorCrm_naoUsaDigitos() {
        // tem letra: nao e CPF nem Prec CP, entao compara so com nome e CRM
        service.listar("123456-SP", null, 0, 20);

        verify(medicoRepository).buscarParaChefe(eq(true), eq("123456-SP"), eq(""), any(Pageable.class));
    }

    @Test
    void listar_curingasDoLike_valemComoTextoLiteral() {
        service.listar("100%_a!", null, 0, 20);

        verify(medicoRepository).buscarParaChefe(eq(true), eq("100!%!_a!!"), eq(""), any(Pageable.class));
    }

    @Test
    void listar_perfilErrado_lanca403SemConsultar() {
        chefe.setPerfil(PerfilUsuario.MEDICO);

        assertThatThrownBy(() -> service.listar(null, "xyz", 0, 20)).isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(medicoRepository);
    }

    @Test
    void listar_chefeInativo_lanca403() {
        chefe.setAtivo(false);

        assertThatThrownBy(() -> service.listar(null, null, 0, 20)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void listar_usuarioInexistente_lanca401() {
        when(usuarioRepository.findByLogin("chefe.fusex")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listar(null, null, 0, 20)).isInstanceOf(NaoAutenticadoException.class);
    }
}
