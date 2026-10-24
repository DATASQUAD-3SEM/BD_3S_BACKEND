package fatec.fusex.nexus.preGuia;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.preGuia.dto.CorrecaoPreGuiaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class BeneficiarioPreGuiaControllerTest {

    private static final String LOGIN = "52998224725";
    private static final String URL = "/beneficiarios/me/pre-guias/7/correcao";

    private MockMvc mockMvc;

    @Mock
    private PreGuiaService preGuiaService;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new BeneficiarioPreGuiaController(preGuiaService, simulacaoLogin))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void buscarCorrecao_quandoSucesso_retorna200ComContrato() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(preGuiaService.buscarDadosCorrecao(7L, LOGIN)).thenReturn(new CorrecaoPreGuiaResponse(
                "0007",
                "ATENCAO",
                new CorrecaoPreGuiaResponse.BeneficiarioResumo("Antonio Carlos Ferreira", "45872213300"),
                LocalDateTime.of(2026, 9, 1, 10, 0),
                new CorrecaoPreGuiaResponse.ObservacaoResumo("A foto enviada esta ilegivel.",
                        LocalDateTime.of(2026, 9, 2, 14, 0)),
                new CorrecaoPreGuiaResponse.EncaminhamentoResumo("abc-123.jpg", "image/jpeg",
                        LocalDateTime.of(2026, 9, 1, 10, 0)),
                new CorrecaoPreGuiaResponse.OcsResumo(10L, "Laboratorio Exemplo")));

        mockMvc.perform(get(URL).header("X-Usuario-Login", LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("0007"))
                .andExpect(jsonPath("$.status").value("ATENCAO"))
                .andExpect(jsonPath("$.beneficiario.nome").value("Antonio Carlos Ferreira"))
                .andExpect(jsonPath("$.beneficiario.precCp").value("45872213300"))
                .andExpect(jsonPath("$.dataCriacao").exists())
                .andExpect(jsonPath("$.observacao.texto").value("A foto enviada esta ilegivel."))
                .andExpect(jsonPath("$.observacao.dataRegistro").exists())
                .andExpect(jsonPath("$.encaminhamentoAtual.nome").value("abc-123.jpg"))
                .andExpect(jsonPath("$.encaminhamentoAtual.tipo").value("image/jpeg"))
                .andExpect(jsonPath("$.encaminhamentoAtual.dataEnvio").exists())
                .andExpect(jsonPath("$.encaminhamentoAtual.url").doesNotExist())
                .andExpect(jsonPath("$.ocsAtual.id").value(10))
                .andExpect(jsonPath("$.ocsAtual.nome").value("Laboratorio Exemplo"));
    }

    @Test
    void buscarCorrecao_semUsuario_retorna401() throws Exception {
        when(simulacaoLogin.loginAtual()).thenThrow(new NaoAutenticadoException("Nenhum usuario logado"));

        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void buscarCorrecao_comPerfilErrado_retorna403() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(preGuiaService.buscarDadosCorrecao(7L, "11144477735"))
                .thenThrow(new AcessoNegadoException("Acesso negado: este endpoint e exclusivo do perfil BENEFICIARIO"));

        mockMvc.perform(get(URL).header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarCorrecao_origemMedico_retorna403() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .thenThrow(new AcessoNegadoException("Pre-guia criada por medico nao pode ser alterada pelo beneficiario"));

        mockMvc.perform(get(URL).header("X-Usuario-Login", LOGIN))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscarCorrecao_deOutroBeneficiario_retorna404() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .thenThrow(new RecursoNaoEncontradoException("Pre-guia nao encontrada: id 7"));

        mockMvc.perform(get(URL).header("X-Usuario-Login", LOGIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscarCorrecao_statusDiferenteDeAtencao_retorna409() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN);
        when(preGuiaService.buscarDadosCorrecao(7L, LOGIN))
                .thenThrow(new ConflitoException("A pre-guia nao esta em Atencao"));

        mockMvc.perform(get(URL).header("X-Usuario-Login", LOGIN))
                .andExpect(status().isConflict());
    }
}
