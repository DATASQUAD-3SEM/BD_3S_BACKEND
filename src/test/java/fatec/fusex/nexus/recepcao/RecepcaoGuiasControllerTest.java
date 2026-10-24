package fatec.fusex.nexus.recepcao;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.recepcao.dto.GuiaRecepcaoResponse;

class RecepcaoGuiasControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RecepcaoGuiasService recepcaoGuiasService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RecepcaoGuiasController(recepcaoGuiasService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void buscar_devolve200ComOsCamposDoContrato() throws Exception {
        when(recepcaoGuiasService.buscar("Antonio")).thenReturn(List.of(new GuiaRecepcaoResponse(
                1L, "202610701", "Antonio Carlos Ferreira", "45872213300",
                "Hospital Exemplo", LocalDate.of(2026, 12, 22))));

        mockMvc.perform(get("/recepcao/guias").param("busca", "Antonio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guiaId").value(1))
                .andExpect(jsonPath("$[0].numero").value("202610701"))
                .andExpect(jsonPath("$[0].beneficiarioNome").value("Antonio Carlos Ferreira"))
                .andExpect(jsonPath("$[0].precCp").value("45872213300"))
                .andExpect(jsonPath("$[0].ocsNome").value("Hospital Exemplo"))
                .andExpect(jsonPath("$[0].validadeAte").value("2026-12-22"));
    }

    @Test
    void buscar_semResultado_devolve200ComListaVazia() throws Exception {
        when(recepcaoGuiasService.buscar("Zezinho")).thenReturn(List.of());

        mockMvc.perform(get("/recepcao/guias").param("busca", "Zezinho"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void buscar_termoCurto_devolve400NoFormatoPadrao() throws Exception {
        when(recepcaoGuiasService.buscar("ab"))
                .thenThrow(new RegraDeNegocioException("Informe ao menos 3 caracteres para buscar"));

        mockMvc.perform(get("/recepcao/guias").param("busca", "ab"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensagem").value("Informe ao menos 3 caracteres para buscar"));
    }

    @Test
    void buscar_semParametro_devolve400() throws Exception {
        when(recepcaoGuiasService.buscar(null))
                .thenThrow(new RegraDeNegocioException("Informe ao menos 3 caracteres para buscar"));

        mockMvc.perform(get("/recepcao/guias"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buscar_perfilErrado_devolve403NoFormatoPadrao() throws Exception {
        when(recepcaoGuiasService.buscar(anyString()))
                .thenThrow(new AcessoNegadoException("Acesso restrito a recepcao"));

        mockMvc.perform(get("/recepcao/guias").param("busca", "Antonio"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}