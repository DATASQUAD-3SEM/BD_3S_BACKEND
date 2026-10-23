package fatec.fusex.nexus.medicoFusex;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
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

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MedicoFusexController(service))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void me_comMedicoLogado_retorna200ComDados() throws Exception {
        when(service.buscarLogado("11144477735")).thenReturn(
                new MedicoMeResponse(7L, "Carlos Eduardo Lima", "Capitao", "123456-SP", "11122233344"));

        mockMvc.perform(get("/medicos/me")
                        .principal(() -> "11144477735")
                        .with(request -> { request.addUserRole("MEDICO"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.nome").value("Carlos Eduardo Lima"));
    }

    @Test
    void me_semPerfilMedico_retorna403() throws Exception {
        mockMvc.perform(get("/medicos/me")
                        .principal(() -> "52998224725")
                        .with(request -> { request.addUserRole("BENEFICIARIO"); return request; }))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_semUsuarioAutenticado_retorna401() throws Exception {
        mockMvc.perform(get("/medicos/me")
                        .with(request -> { request.addUserRole("MEDICO"); return request; }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_quandoMedicoNaoExiste_retorna404() throws Exception {
        when(service.buscarLogado("11144477735"))
                .thenThrow(new fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException("Medico nao encontrado"));

        mockMvc.perform(get("/medicos/me")
                        .principal(() -> "11144477735")
                        .with(request -> { request.addUserRole("MEDICO"); return request; }))
                .andExpect(status().isNotFound());
    }
}