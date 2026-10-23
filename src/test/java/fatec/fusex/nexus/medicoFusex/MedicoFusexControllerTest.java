package fatec.fusex.nexus.medicoFusex;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.medicoFusex.dto.ProcedimentoMedicoResponse;
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
    void buscarProcedimentos_comTermoValido_retornaLista() throws Exception {
        ProcedimentoMedicoResponse proc = new ProcedimentoMedicoResponse("40304361", "Hemograma completo");
        when(service.buscar("hemo")).thenReturn(List.of(proc));

        mockMvc.perform(get("/medicos/procedimentos")
                        .param("busca", "hemo")
                        .with(request -> { request.addUserRole("MEDICO"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigoTuss").value("40304361"))
                .andExpect(jsonPath("$[0].nome").value("Hemograma completo"));
    }

    @Test
    void buscarProcedimentos_comTermoCurto_retorna400() throws Exception {
        when(service.buscar("a")).thenThrow(new RegraDeNegocioException("A busca deve conter no mínimo 2 caracteres."));

        mockMvc.perform(get("/medicos/procedimentos")
                        .param("busca", "a")
                        .with(request -> { request.addUserRole("MEDICO"); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A busca deve conter no mínimo 2 caracteres."));
    }

    @Test
    void buscarProcedimentos_semPerfilMedico_retorna403() throws Exception {
        mockMvc.perform(get("/medicos/procedimentos")
                        .param("busca", "hemo")
                        .with(request -> { request.addUserRole("BENEFICIARIO"); return request; }))
                .andExpect(status().isForbidden());
    }
}