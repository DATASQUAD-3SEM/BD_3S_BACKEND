package fatec.fusex.nexus.medicoFusex;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MedicoFusexControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicoFusexService service;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        // Instancia o Controller passando os mocks
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MedicoFusexController(service, simulacaoLogin))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void me_comMedicoLogado_retorna200ComDados() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(service.buscarLogado("11144477735")).thenReturn(
                new MedicoMeResponse(7L, "Carlos Eduardo Lima", "Capitao", "123456-SP"));

        mockMvc.perform(get("/medicos/me")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nome").value("Carlos Eduardo Lima"));
    }

    @Test
    void me_semUsuarioAutenticado_retorna401() throws Exception {
        when(simulacaoLogin.loginAtual()).thenThrow(new NaoAutenticadoException("Nenhum usuario logado"));

        mockMvc.perform(get("/medicos/me"))
                .andExpect(status().isUnauthorized());
    }
}