package fatec.fusex.nexus.medicoFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoPerfilResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Testes da leitura dos dados profissionais do medico logado (T11-BE1).
 */
@ExtendWith(MockitoExtension.class)
class MedicoPerfilServiceTest {

    private static final String CPF = "52998224725";
    private static final Long USUARIO_ID = 7L;

    @Mock
    private MedicoFusexRepository medicoFusexRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    private MedicoPerfilService service;

    @BeforeEach
    void configurar() {
        service = new MedicoPerfilService(
                medicoFusexRepository,
                usuarioRepository,
                simulacaoLogin);
    }

    @Test
    void buscarPerfilLogado_quandoMedicoCadastrado_retornaPerfil() {
        Usuario usuario = usuarioMedico();
        usuario.setAssinaturaUrl("assinaturas/medico.png");

        MedicoFusex medico = medico(usuario);

        prepararUsuario(usuario);
        when(medicoFusexRepository.findById(USUARIO_ID))
                .thenReturn(Optional.of(medico));

        MedicoPerfilResponse resposta = service.buscarPerfilLogado();

        assertThat(resposta.nome()).isEqualTo("Carlos Alberto");
        assertThat(resposta.patente()).isEqualTo("Capitao");
        assertThat(resposta.cpf()).isEqualTo(CPF);
        assertThat(resposta.precCp()).isEqualTo("45872213300");
        assertThat(resposta.crm()).isEqualTo("CRM-SP-12345");
        assertThat(resposta.assinaturaCadastrada()).isTrue();
    }

    @Test
    void buscarPerfilLogado_quandoSemAssinatura_retornaFalse() {
        Usuario usuario = usuarioMedico();
        MedicoFusex medico = medico(usuario);

        prepararUsuario(usuario);
        when(medicoFusexRepository.findById(USUARIO_ID))
                .thenReturn(Optional.of(medico));

        MedicoPerfilResponse resposta = service.buscarPerfilLogado();

        assertThat(resposta.assinaturaCadastrada()).isFalse();
    }

    @Test
    void buscarPerfilLogado_quandoPerfilErrado_lancaAcessoNegado() {
        Usuario beneficiario = usuarioMedico();
        beneficiario.setPerfil(PerfilUsuario.BENEFICIARIO);

        prepararUsuario(beneficiario);

        assertThatThrownBy(() -> service.buscarPerfilLogado())
                .isInstanceOf(AcessoNegadoException.class);

        verifyNoInteractions(medicoFusexRepository);
    }

    @Test
    void buscarPerfilLogado_quandoUsuarioNaoExiste_lancaNaoAutenticado() {
        when(simulacaoLogin.loginAtual()).thenReturn(CPF);
        when(usuarioRepository.findByLogin(CPF))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPerfilLogado())
                .isInstanceOf(NaoAutenticadoException.class);

        verifyNoInteractions(medicoFusexRepository);
    }

    @Test
    void buscarPerfilLogado_quandoMedicoNaoTemCadastro_lanca404() {
        Usuario usuario = usuarioMedico();

        prepararUsuario(usuario);
        when(medicoFusexRepository.findById(USUARIO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPerfilLogado())
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private void prepararUsuario(Usuario usuario) {
        when(simulacaoLogin.loginAtual()).thenReturn(CPF);
        when(usuarioRepository.findByLogin(CPF))
                .thenReturn(Optional.of(usuario));
    }

    private Usuario usuarioMedico() {
        Usuario usuario = new Usuario();
        usuario.setIdUsuario(USUARIO_ID);
        usuario.setLogin(CPF);
        usuario.setPerfil(PerfilUsuario.MEDICO);
        usuario.setAtivo(true);
        return usuario;
    }

    private MedicoFusex medico(Usuario usuario) {
        MedicoFusex medico = new MedicoFusex();
        medico.setUsuarioId(USUARIO_ID);
        medico.setUsuario(usuario);
        medico.setNome("Carlos Alberto");
        medico.setPatente("Capitao");
        medico.setPrecCp("45872213300");
        medico.setCrm("CRM-SP-12345");
        return medico;
    }
}