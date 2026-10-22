package fatec.fusex.nexus.ocs;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.ocs.dto.OcsBuscaResponse;
import fatec.fusex.nexus.ocs.dto.OcsResponse;

class OcsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OcsService ocsService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OcsController(ocsService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void buscar_semTermo_deveRetornarListaInicial() throws Exception {
        OcsResponse primeira = new OcsResponse(1L, "CTR-001", "Laboratorio X", "OCS");
        OcsResponse segunda = new OcsResponse(2L, "CTR-002", "Hospital Y", "PSA");

        when(ocsService.buscar(isNull())).thenReturn(new OcsBuscaResponse(List.of(primeira, segunda), 2L));

        // AQUI: usamos .addUserRole("MEDICO") para simular o perfil do usuário logado
        mockMvc.perform(get("/ocs")
                        .with(request -> { request.addUserRole("MEDICO"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.resultados", hasSize(2)))
                .andExpect(jsonPath("$.resultados[0].nome").value("Laboratorio X"))
                .andExpect(jsonPath("$.resultados[1].nome").value("Hospital Y"));

        verify(ocsService).buscar(isNull());
    }

    @Test
    void buscar_comTermo_deveFiltrarPeloParametro() throws Exception {
        OcsResponse primeira = new OcsResponse(1L, "CTR-001", "Laboratorio X", "OCS");

        when(ocsService.buscar("lab")).thenReturn(new OcsBuscaResponse(List.of(primeira), 1L));

        mockMvc.perform(get("/ocs")
                        .param("busca", "lab")
                        .with(request -> { request.addUserRole("MEDICO"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.resultados", hasSize(1)))
                .andExpect(jsonPath("$.resultados[0].nome").value("Laboratorio X"));

        verify(ocsService).buscar("lab");
    }

    @Test
    void buscar_quandoNaoEncontra_deveRetornarListaVazia() throws Exception {
        when(ocsService.buscar(anyString())).thenReturn(new OcsBuscaResponse(List.of(), 0L));

        mockMvc.perform(get("/ocs")
                        .param("busca", "inexistente")
                        .with(request -> { request.addUserRole("MEDICO"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.resultados", hasSize(0)));
    }
}