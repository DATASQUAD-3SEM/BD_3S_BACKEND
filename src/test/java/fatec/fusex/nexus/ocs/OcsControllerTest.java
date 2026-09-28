package fatec.fusex.nexus.ocs;

import static org.hamcrest.Matchers.hasSize;
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
    void listar_deveRetornarListaDeOcs() throws Exception {
        Ocs primeira = new Ocs();
        primeira.setNome("Laboratorio X");
        primeira.setContratoNum("CTR-001");
        primeira.setTipo("Laboratorio");

        Ocs segunda = new Ocs();
        segunda.setNome("Hospital Y");
        segunda.setContratoNum("CTR-002");
        segunda.setTipo("Hospital");

        when(ocsService.listar()).thenReturn(List.of(primeira, segunda));

        mockMvc.perform(get("/ocs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].nome").value("Laboratorio X"))
                .andExpect(jsonPath("$[1].nome").value("Hospital Y"));

        verify(ocsService).listar();
    }

    @Test
    void listar_quandoNaoExistemOcs_deveRetornarListaVazia() throws Exception {
        when(ocsService.listar()).thenReturn(List.of());

        mockMvc.perform(get("/ocs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}