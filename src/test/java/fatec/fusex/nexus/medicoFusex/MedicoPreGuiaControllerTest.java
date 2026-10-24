package fatec.fusex.nexus.medicoFusex;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.preGuia.MedicoPreGuiaService;
import fatec.fusex.nexus.preGuia.dto.CriarPreGuiaMedicoRequest;
import fatec.fusex.nexus.preGuia.dto.PreGuiaMedicoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MedicoPreGuiaControllerTest {

    private static final String URL = "/medicos/pre-guias";
    private static final String CORPO =
            "{\"beneficiarioId\":5,\"ocsId\":10,\"procedimentos\":[\"40304361\",\"40302040\"]}";

    private MockMvc mockMvc;

    @Mock
    private MedicoPreGuiaService medicoPreGuiaService;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MedicoPreGuiaController(medicoPreGuiaService, simulacaoLogin))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void criar_comMedicoLogado_retorna201ComIdNumeroStatusEData() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(medicoPreGuiaService.criar(eq("11144477735"), any())).thenReturn(
                new PreGuiaMedicoResponse(7L, "0007", "PENDENTE", LocalDateTime.of(2026, 10, 24, 10, 0)));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CORPO)
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.numero").value("0007"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataCriacao").exists());

        verify(medicoPreGuiaService).criar("11144477735",
                new CriarPreGuiaMedicoRequest(5L, 10L, List.of("40304361", "40302040")));
    }

    @Test
    void criar_dadosInvalidos_retorna422ApontandoOCampo() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(medicoPreGuiaService.criar(eq("11144477735"), any()))
                .thenThrow(new DadosInvalidosException("procedimentos", "Inclua ao menos um procedimento"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"beneficiarioId\":5,\"ocsId\":10,\"procedimentos\":[]}")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value("procedimentos: Inclua ao menos um procedimento"));
    }

    @Test
    void criar_comPerfilErrado_retorna403() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("52998224725");
        when(medicoPreGuiaService.criar(eq("52998224725"), any()))
                .thenThrow(new AcessoNegadoException("Acesso restrito a medicos"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CORPO)
                        .header("X-Usuario-Login", "52998224725"))
                .andExpect(status().isForbidden());
    }

    @Test
    void criar_semUsuario_retorna401SemChamarOService() throws Exception {
        when(simulacaoLogin.loginAtual()).thenThrow(new NaoAutenticadoException("Nenhum usuario logado"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(CORPO))
                .andExpect(status().isUnauthorized());

        verify(medicoPreGuiaService, never()).criar(any(), any());
    }
}
