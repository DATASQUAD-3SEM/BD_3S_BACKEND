package fatec.fusex.nexus.ocs;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.medicoFusex.MedicoFusexService;
import fatec.fusex.nexus.ocs.dto.OcsBuscaResponse;
import fatec.fusex.nexus.ocs.dto.OcsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Testes HTTP de GET /ocs?busca= (T2-BE2).
 */
class OcsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OcsService ocsService;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @Mock
    private MedicoFusexService medicoService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OcsController(ocsService, simulacaoLogin, medicoService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void buscar_semTermo_deveRetornarListaInicialComCamposDoContrato() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");

        OcsResponse primeira = new OcsResponse(1L, "CTR-001", "Laboratorio X", "OCS");
        OcsResponse segunda = new OcsResponse(2L, "CTR-002", "Hospital Y", "PSA");
        when(ocsService.buscar(isNull())).thenReturn(new OcsBuscaResponse(List.of(primeira, segunda), 2L));

        mockMvc.perform(get("/ocs")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resultados", hasSize(2)))
                .andExpect(jsonPath("$.resultados[0].id").value(1))
                .andExpect(jsonPath("$.resultados[0].nome").value("Laboratorio X"))
                .andExpect(jsonPath("$.resultados[0].contrato").value("CTR-001"))
                .andExpect(jsonPath("$.resultados[0].tipo").value("OCS"))
                .andExpect(jsonPath("$.resultados[0].contratoNum").doesNotExist());

        verify(medicoService).validarPerfilMedico("11144477735");
    }

    @Test
    void buscar_comTermo_repassaOTermoParaOService() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        OcsResponse lab = new OcsResponse(1L, "CTR-001", "Laboratorio X", "OCS");
        when(ocsService.buscar("lab")).thenReturn(new OcsBuscaResponse(List.of(lab), 1L));

        mockMvc.perform(get("/ocs").param("busca", "lab")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.resultados", hasSize(1)));

        verify(ocsService).buscar("lab");
    }

    @Test
    void buscar_totalMaiorQueOsItensRetornados_mantemOTotalGeral() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        List<OcsResponse> trinta = java.util.stream.IntStream.rangeClosed(1, 30)
                .mapToObj(i -> new OcsResponse((long) i, "CTR-" + i, "OCS " + i, "OCS"))
                .toList();
        when(ocsService.buscar(isNull())).thenReturn(new OcsBuscaResponse(trinta, 35L));

        mockMvc.perform(get("/ocs").header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(35))
                .andExpect(jsonPath("$.resultados", hasSize(30)));
    }

    @Test
    void buscar_comPerfilErrado_retorna403SemConsultarOcs() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("52998224725"); // login de beneficiário
        when(medicoService.validarPerfilMedico("52998224725"))
                .thenThrow(new AcessoNegadoException("Acesso restrito a medicos"));

        mockMvc.perform(get("/ocs")
                        .header("X-Usuario-Login", "52998224725"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ocsService);
    }

    @Test
    void buscar_semUsuario_retorna401SemConsultarOcs() throws Exception {
        when(simulacaoLogin.loginAtual())
                .thenThrow(new NaoAutenticadoException("Nenhum usuario logado"));

        mockMvc.perform(get("/ocs"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(medicoService, ocsService);
    }
}
