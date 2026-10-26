package fatec.fusex.nexus.medicoFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Testes das regras comuns do perfil medico: medico valido, perfil errado, inativo e sem cadastro.
 */
class MedicoFusexServiceTest {

    private static final String LOGIN = "11144477735";

    private MedicoFusexService service;

    @Mock
    private MedicoFusexRepository medicoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new MedicoFusexService(medicoRepository, usuarioRepository);
    }

    private Usuario usuario(PerfilUsuario perfil, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(7L);
        usuario.setLogin(LOGIN);
        usuario.setPerfil(perfil);
        usuario.setAtivo(ativo);
        return usuario;
    }

    private MedicoFusex medico(Usuario usuario) {
        MedicoFusex medico = new MedicoFusex();
        medico.setUsuarioId(7L);
        medico.setUsuario(usuario);
        medico.setNome("Carlos Eduardo Lima");
        medico.setPatente("Capitao");
        medico.setCrm("123456-SP");
        medico.setPrecCp("11122233344");
        return medico;
    }

    // ===== buscarLogado (GET /medicos/me) =====

    @Test
    void buscarLogado_quandoMedicoAtivoComCadastro_retornaDados() {
        Usuario usuario = usuario(PerfilUsuario.MEDICO, true);
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario));
        when(medicoRepository.findByUsuarioLogin(LOGIN)).thenReturn(Optional.of(medico(usuario)));

        MedicoMeResponse resposta = service.buscarLogado(LOGIN);

        assertThat(resposta.nome()).isEqualTo("Carlos Eduardo Lima");
        assertThat(resposta.patente()).isEqualTo("Capitao");
        assertThat(resposta.crm()).isEqualTo("123456-SP");
    }

    @Test
    void buscarLogado_quandoMedicoSemCadastro_lancaRecursoNaoEncontrado() {
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(usuario(PerfilUsuario.MEDICO, true)));
        when(medicoRepository.findByUsuarioLogin(LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarLogado(LOGIN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarLogado_quandoPerfilErrado_lancaAcessoNegadoSemBuscarMedico() {
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(usuario(PerfilUsuario.BENEFICIARIO, true)));

        assertThatThrownBy(() -> service.buscarLogado(LOGIN))
                .isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(medicoRepository);
    }

    @Test
    void buscarLogado_quandoUsuarioInativo_lancaAcessoNegado() {
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(usuario(PerfilUsuario.MEDICO, false)));

        assertThatThrownBy(() -> service.buscarLogado(LOGIN))
                .isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(medicoRepository);
    }

    @Test
    void buscarLogado_quandoUsuarioNaoExiste_lancaAcessoNegado() {
        when(usuarioRepository.findByLogin("00000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarLogado("00000000000"))
                .isInstanceOf(AcessoNegadoException.class);
        verifyNoInteractions(medicoRepository);
    }

    // ===== validarPerfilMedico (usado por GET /ocs) =====

    @Test
    void validarPerfilMedico_medicoAtivoSemCadastro_passa() {
        // /ocs so exige o perfil; o cadastro em MedicoFusex nao e consultado
        Usuario usuario = usuario(PerfilUsuario.MEDICO, true);
        when(usuarioRepository.findByLogin(LOGIN)).thenReturn(Optional.of(usuario));

        assertThat(service.validarPerfilMedico(LOGIN)).isSameAs(usuario);
        verifyNoInteractions(medicoRepository);
    }

    @Test
    void validarPerfilMedico_beneficiario_lancaAcessoNegado() {
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(usuario(PerfilUsuario.BENEFICIARIO, true)));

        assertThatThrownBy(() -> service.validarPerfilMedico(LOGIN))
                .isInstanceOf(AcessoNegadoException.class)
                .hasMessageContaining("Acesso restrito a medicos");
    }

    @Test
    void validarPerfilMedico_medicoInativo_lancaAcessoNegado() {
        when(usuarioRepository.findByLogin(LOGIN))
                .thenReturn(Optional.of(usuario(PerfilUsuario.MEDICO, false)));

        assertThatThrownBy(() -> service.validarPerfilMedico(LOGIN))
                .isInstanceOf(AcessoNegadoException.class);
    }
}
