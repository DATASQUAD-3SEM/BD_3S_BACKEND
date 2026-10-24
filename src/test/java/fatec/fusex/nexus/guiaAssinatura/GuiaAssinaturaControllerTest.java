package fatec.fusex.nexus.guiaAssinatura;

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

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.guiaAssinatura.dto.GuiaParaAssinarResponse;

class GuiaAssinaturaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GuiaAssinaturaService guiaAssinaturaService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GuiaAssinaturaController(guiaAssinaturaService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listar_devolve200ComOsCamposDoContrato() throws Exception {
        when(guiaAssinaturaService.listarParaAssinar()).thenReturn(List.of(
                new GuiaParaAssinarResponse(
                        1L, "202610699", "Joao Pereira Alves",
                        List.of(AssinaturaGuiaPapel.MEDICO, AssinaturaGuiaPapel.CHEFE))));

        mockMvc.perform(get("/guias/para-assinar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].guiaId").value(1))
                .andExpect(jsonPath("$[0].numero").value("202610699"))
                .andExpect(jsonPath("$[0].beneficiarioNome").value("Joao Pereira Alves"))
                .andExpect(jsonPath("$[0].pendencias[0]").value("MEDICO"))
                .andExpect(jsonPath("$[0].pendencias[1]").value("CHEFE"));
    }

    @Test
    void listar_semGuias_devolve200ComListaVazia() throws Exception {
        when(guiaAssinaturaService.listarParaAssinar()).thenReturn(List.of());

        mockMvc.perform(get("/guias/para-assinar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void listar_perfilSemPermissao_devolve403NoFormatoPadrao() throws Exception {
        when(guiaAssinaturaService.listarParaAssinar())
                .thenThrow(new AcessoNegadoException("Acesso restrito a medicos e chefe do FUSEX"));

        mockMvc.perform(get("/guias/para-assinar"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}