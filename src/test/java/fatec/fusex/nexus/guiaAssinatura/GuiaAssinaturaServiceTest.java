package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaAssinatura.dto.GuiaParaAssinarResponse;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Testes da lista de Guias para assinar (T5-BE1): pendencias por papel, lista vazia e perfil errado.
 */
class GuiaAssinaturaServiceTest {

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private AssinaturaGuiaRepository assinaturaGuiaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    private GuiaAssinaturaService service;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new GuiaAssinaturaService(
                guiaRepository, assinaturaGuiaRepository, new UsuarioLogado(simulacaoLogin, usuarioRepository));
        when(simulacaoLogin.loginAtual()).thenReturn("usuario");
    }

    @Test
    void medico_consultaGuiasSemAssinaturaDeMedico_eCalculaPendencias() {
        Guia semAssinaturas = guia(1L, "202610699", "Joao Pereira Alves", LocalDateTime.of(2026, 10, 1, 9, 0));
        Guia comChefe = guia(2L, "202610701", "Marta Souza Lima", LocalDateTime.of(2026, 10, 2, 9, 0));

        when(usuarioRepository.findByLogin("usuario"))
                .thenReturn(Optional.of(usuario(PerfilUsuario.MEDICO)));
        when(guiaRepository.buscarSemAssinatura(AssinaturaGuiaPapel.MEDICO))
                .thenReturn(List.of(semAssinaturas, comChefe));
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(any()))
                .thenReturn(List.of(assinatura(comChefe, AssinaturaGuiaPapel.CHEFE)));

        List<GuiaParaAssinarResponse> resultado = service.listarParaAssinar();

        assertThat(resultado).extracting(GuiaParaAssinarResponse::numero)
                .containsExactly("202610699", "202610701");
        assertThat(resultado.get(0).pendencias())
                .containsExactly(AssinaturaGuiaPapel.MEDICO, AssinaturaGuiaPapel.CHEFE);
        assertThat(resultado.get(1).pendencias())
                .containsExactly(AssinaturaGuiaPapel.MEDICO);
    }

    @Test
    void chefe_consultaGuiasSemAssinaturaDeChefe_eCalculaPendencias() {
        Guia semAssinaturas = guia(1L, "202610699", "Joao Pereira Alves", LocalDateTime.of(2026, 10, 1, 9, 0));
        Guia comMedico = guia(2L, "202610701", "Marta Souza Lima", LocalDateTime.of(2026, 10, 2, 9, 0));

        when(usuarioRepository.findByLogin("usuario"))
                .thenReturn(Optional.of(usuario(PerfilUsuario.CHEFE_FUSEX)));
        when(guiaRepository.buscarSemAssinatura(AssinaturaGuiaPapel.CHEFE))
                .thenReturn(List.of(semAssinaturas, comMedico));
        when(assinaturaGuiaRepository.findByGuiaIdGuiaIn(any()))
                .thenReturn(List.of(assinatura(comMedico, AssinaturaGuiaPapel.MEDICO)));

        List<GuiaParaAssinarResponse> resultado = service.listarParaAssinar();

        assertThat(resultado.get(0).pendencias())
                .containsExactly(AssinaturaGuiaPapel.MEDICO, AssinaturaGuiaPapel.CHEFE);
        assertThat(resultado.get(1).pendencias())
                .containsExactly(AssinaturaGuiaPapel.CHEFE);
    }

    @Test
    void listaVazia_devolveListaVazia_eNaoConsultaAsAssinaturas() {
        when(usuarioRepository.findByLogin("usuario"))
                .thenReturn(Optional.of(usuario(PerfilUsuario.MEDICO)));
        when(guiaRepository.buscarSemAssinatura(AssinaturaGuiaPapel.MEDICO))
                .thenReturn(List.of());

        assertThat(service.listarParaAssinar()).isEmpty();
        verify(assinaturaGuiaRepository, never()).findByGuiaIdGuiaIn(any());
    }

    @Test
    void perfilSemPermissao_lancaAcessoNegado_eNaoConsultaAGuia() {
        for (PerfilUsuario perfil : List.of(
                PerfilUsuario.BENEFICIARIO, PerfilUsuario.SETOR_GUIAS, PerfilUsuario.RECEPCAO)) {
            when(usuarioRepository.findByLogin("usuario")).thenReturn(Optional.of(usuario(perfil)));

            assertThatThrownBy(() -> service.listarParaAssinar())
                    .isInstanceOf(AcessoNegadoException.class);
        }

        verifyNoInteractions(guiaRepository);
    }

    @Test
    void usuarioNaoCadastrado_lancaNaoAutenticado() {
        when(usuarioRepository.findByLogin("usuario")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listarParaAssinar())
                .isInstanceOf(NaoAutenticadoException.class);

        verifyNoInteractions(guiaRepository);
    }

    private static Usuario usuario(PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setPerfil(perfil);
        return usuario;
    }

    private static Guia guia(Long id, String numero, String nomeBeneficiario, LocalDateTime insercao) {
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setNome(nomeBeneficiario);

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);

        Guia guia = new Guia();
        guia.setIdGuia(id);
        guia.setNumero(numero);
        guia.setPreGuia(preGuia);
        guia.setDataInsercao(insercao);
        return guia;
    }

    private static AssinaturaGuia assinatura(Guia guia, AssinaturaGuiaPapel papel) {
        AssinaturaGuia assinatura = new AssinaturaGuia();
        assinatura.setGuia(guia);
        assinatura.setPapel(papel);
        return assinatura;
    }
}