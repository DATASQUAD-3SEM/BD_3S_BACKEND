package fatec.fusex.nexus.ocs;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.procedimento.ProcedimentoExame;

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

    @Test
    void listarProcedimentos_deveRetornarProcedimentosDaOcs() throws Exception {
        ProcedimentoExame hemograma = new ProcedimentoExame();
        hemograma.setCodigoTuss("40304361");
        hemograma.setTerminologiaProcedimentoEvento("Hemograma Completo");

        ProcedimentoExame glicose = new ProcedimentoExame();
        glicose.setCodigoTuss("40302040");
        glicose.setTerminologiaProcedimentoEvento("Glicose");

        when(ocsService.listarProcedimentos(1L))
                .thenReturn(List.of(hemograma, glicose));

        mockMvc.perform(get("/ocs/1/procedimentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.codigoTuss == '40304361')]").exists())
                .andExpect(jsonPath("$[?(@.codigoTuss == '40302040')]").exists());

        verify(ocsService).listarProcedimentos(1L);
    }

    @Test
    void listarProcedimentos_quandoOcsNaoExiste_deveRetornar404() throws Exception {
        when(ocsService.listarProcedimentos(999L))
                .thenThrow(new RecursoNaoEncontradoException("OCS nao encontrada: id 999"));

        mockMvc.perform(get("/ocs/999/procedimentos"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Not Found"))
                .andExpect(jsonPath("$.mensagem").value("OCS nao encontrada: id 999"));
    }
}