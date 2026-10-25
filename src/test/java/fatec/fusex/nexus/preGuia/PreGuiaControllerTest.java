package fatec.fusex.nexus.preGuia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.preGuia.dto.PreGuiaResponse;

/**
 * Testes HTTP de POST /pre-guias (criacao da pre-guia pelo beneficiario logado), com o servico simulado.
 */
@WebMvcTest(PreGuiaController.class)
@Import(SimulacaoLogin.class)
class PreGuiaControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private PreGuiaService preGuiaService;

    PreGuiaControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    private final MockMultipartFile arquivo =
            new MockMultipartFile("arquivo", "encaminhamento.pdf", "application/pdf", "%PDF-1.4".getBytes());

    @Test
    void criar_quandoRequisicaoValida_devolve201ComNumero() throws Exception {
        when(preGuiaService.criar(eq(10L), any())).thenReturn(
                new PreGuiaResponse(7L, "0007", "PENDENTE", LocalDateTime.of(2026, 9, 24, 10, 0), 10L));

        mockMvc.perform(multipart("/pre-guias").file(arquivo).param("ocsId", "10")
                        .header("X-Usuario-Login", "52998224725"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("0007"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.ocsId").value(10));
    }

    @Test
    void criar_dadosInvalidos_devolve422ApontandoOCampo() throws Exception {
        when(preGuiaService.criar(any(), any())).thenThrow(new DadosInvalidosException("ocsId", "Escolha a OCS"));

        mockMvc.perform(multipart("/pre-guias").file(arquivo).header("X-Usuario-Login", "52998224725"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.containsString("ocsId")));
    }

    @Test
    void criar_perfilErrado_devolve403() throws Exception {
        when(preGuiaService.criar(any(), any())).thenThrow(new AcessoNegadoException("Acesso negado"));

        mockMvc.perform(multipart("/pre-guias").file(arquivo).param("ocsId", "10")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isForbidden());
    }
}
