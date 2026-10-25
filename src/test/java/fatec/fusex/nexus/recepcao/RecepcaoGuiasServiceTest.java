package fatec.fusex.nexus.recepcao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.recepcao.dto.GuiaRecepcaoResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

class RecepcaoGuiasServiceTest {

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    private RecepcaoGuiasService service;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        service = new RecepcaoGuiasService(guiaRepository, new UsuarioLogado(simulacaoLogin, usuarioRepository));

        when(simulacaoLogin.loginAtual()).thenReturn("recepcao");
        when(usuarioRepository.findByLogin("recepcao"))
                .thenReturn(Optional.of(usuario(PerfilUsuario.RECEPCAO)));
    }

    @Test
    void buscar_porNome_usaTermoSemEspacosSemPrecCpELimite50() {
        when(guiaRepository.buscarConcluidasParaRecepcao(
                eq(PreGuiaStatus.CONCLUIDA), eq("Antonio"), isNull(), any(Pageable.class)))
                .thenReturn(List.of(guia(1L, "202610701", LocalDate.of(2026, 10, 22).atStartOfDay())));

        List<GuiaRecepcaoResponse> resultado = service.buscar("  Antonio  ");

        assertThat(resultado).hasSize(1);
        ArgumentCaptor<Pageable> limite = ArgumentCaptor.forClass(Pageable.class);
        verify(guiaRepository).buscarConcluidasParaRecepcao(
                eq(PreGuiaStatus.CONCLUIDA), eq("Antonio"), isNull(), limite.capture());
        assertThat(limite.getValue().getPageSize()).isEqualTo(50);
    }
    @Test
    void buscar_precCpComMascara_enviaSoOsDigitosComoPrecCp() {
        when(guiaRepository.buscarConcluidasParaRecepcao(
                eq(PreGuiaStatus.CONCLUIDA), eq("458.722.133-00"), eq("45872213300"), any(Pageable.class)))
                .thenReturn(List.of());

        List<GuiaRecepcaoResponse> resultado = service.buscar("458.722.133-00");

        assertThat(resultado).isEmpty();
        verify(guiaRepository).buscarConcluidasParaRecepcao(
                eq(PreGuiaStatus.CONCLUIDA), eq("458.722.133-00"), eq("45872213300"), any(Pageable.class));
    }

    @Test
    void buscar_termoComLetrasEDigitos_naoViraPrecCp() {
        when(guiaRepository.buscarConcluidasParaRecepcao(
                eq(PreGuiaStatus.CONCLUIDA), eq("Antonio 1"), isNull(), any(Pageable.class)))
                .thenReturn(List.of());

        service.buscar("Antonio 1");

        verify(guiaRepository).buscarConcluidasParaRecepcao(
                eq(PreGuiaStatus.CONCLUIDA), eq("Antonio 1"), isNull(), any(Pageable.class));
    }

    @Test
    void buscar_termoCurto_lancaRegraDeNegocio_enaoConsultaORepositorio() {
        assertThatThrownBy(() -> service.buscar("ab"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Informe ao menos 3 caracteres para buscar");
        assertThatThrownBy(() -> service.buscar("   "))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> service.buscar(null))
                .isInstanceOf(RegraDeNegocioException.class);

        verifyNoInteractions(guiaRepository);
    }

    @Test
    void buscar_comPerfilQueNaoEhRecepcao_lancaAcessoNegado_antesDeValidarOTermo() {
        for (PerfilUsuario perfil : PerfilUsuario.values()) {
            if (perfil == PerfilUsuario.RECEPCAO) {
                continue;
            }
            when(usuarioRepository.findByLogin("recepcao")).thenReturn(Optional.of(usuario(perfil)));

            assertThatThrownBy(() -> service.buscar("ab"))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        verifyNoInteractions(guiaRepository);
    }

    @Test
    void buscar_usuarioLogadoInexistente_lancaNaoAutenticado() {
        when(usuarioRepository.findByLogin("recepcao")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar("antonio"))
                .isInstanceOf(NaoAutenticadoException.class);

        verifyNoInteractions(guiaRepository);
    }

    @Test
    void buscar_mapeiaOsCamposDoContrato_evalidadeEDoisMesesDepoisDaInsercao() {
        when(guiaRepository.buscarConcluidasParaRecepcao(
                eq(PreGuiaStatus.CONCLUIDA), eq("Antonio"), isNull(), any(Pageable.class)))
                .thenReturn(List.of(guia(7L, "202610701",
                        LocalDate.of(2026, 10, 22).atStartOfDay())));

        GuiaRecepcaoResponse r = service.buscar("Antonio").get(0);

        assertThat(r.guiaId()).isEqualTo(7L);
        assertThat(r.numero()).isEqualTo("202610701");
        assertThat(r.beneficiarioNome()).isEqualTo("Antonio Carlos Ferreira");
        assertThat(r.precCp()).isEqualTo("45872213300");
        assertThat(r.ocsNome()).isEqualTo("Hospital Exemplo");
        assertThat(r.validadeAte()).isEqualTo(LocalDate.of(2026, 12, 22));
    }

    @Test
    void extrairPrecCp_aceitaComOuSemMascara_enaoAceitaTermoMisto() {
        assertThat(RecepcaoGuiasService.extrairPrecCp("45872213300")).isEqualTo("45872213300");
        assertThat(RecepcaoGuiasService.extrairPrecCp("458.722.133-00")).isEqualTo("45872213300");
        assertThat(RecepcaoGuiasService.extrairPrecCp("Antonio")).isNull();
        assertThat(RecepcaoGuiasService.extrairPrecCp("...---")).isNull();
    }

    private static Usuario usuario(PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin("recepcao");
        usuario.setPerfil(perfil);
        return usuario;
    }

    private static Guia guia(Long id, String numero, java.time.LocalDateTime insercao) {
        Beneficiario beneficiario = new Beneficiario();
        beneficiario.setNome("Antonio Carlos Ferreira");
        beneficiario.setPrecCp("45872213300");

        Ocs ocs = new Ocs();
        ocs.setNome("Hospital Exemplo");

        PreGuia preGuia = new PreGuia();
        preGuia.setBeneficiario(beneficiario);
        preGuia.setOcs(ocs);
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);

        Guia guia = new Guia();
        guia.setIdGuia(id);
        guia.setNumero(numero);
        guia.setPreGuia(preGuia);
        guia.setDataInsercao(insercao);
        return guia;
    }
}