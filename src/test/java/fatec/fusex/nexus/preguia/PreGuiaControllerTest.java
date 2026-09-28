package fatec.fusex.nexus.preguia;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.preguia.dto.PreGuiaResponse;

@WebMvcTest(PreGuiaController.class)
class PreGuiaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreGuiaService preGuiaService;

    @Test
    void criar_quandoRequisicaoValida_devolve201ComoCorpoDaPreGuia() throws Exception {
        PreGuiaResponse resposta = new PreGuiaResponse(
                1L,
                "PENDENTE",
                LocalDateTime.of(2026, 9, 24, 10, 0),
                "encaminhamentos/gerado.pdf",
                10L);

        when(preGuiaService.criar(anyString(), anyString(), anyLong(), any()))
                .thenReturn(resposta);

        MockMultipartFile arquivo =
                new MockMultipartFile("arquivo", "encaminhamento.pdf", "application/pdf", "conteudo".getBytes());

        mockMvc.perform(multipart("/pre-guias")
                        .file(arquivo)
                        .param("cpf", "12345678901")
                        .param("precCp", "0001112223")
                        .param("ocsId", "10"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.encaminhamentoUrl").value("encaminhamentos/gerado.pdf"))
                .andExpect(jsonPath("$.ocsId").value(10));
    }

    @Test
    void criar_quandoServiceRecusa_devolve400() throws Exception {
        when(preGuiaService.criar(anyString(), anyString(), anyLong(), any()))
                .thenThrow(new RegraDeNegocioException("E necessario anexar o encaminhamento medico"));

        MockMultipartFile arquivo =
                new MockMultipartFile("arquivo", "encaminhamento.pdf", "application/pdf", "conteudo".getBytes());

        mockMvc.perform(multipart("/pre-guias")
                        .file(arquivo)
                        .param("cpf", "12345678901")
                        .param("precCp", "0001112223")
                        .param("ocsId", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criar_quandoOcsIdAusente_devolve400() throws Exception {
        MockMultipartFile arquivo =
                new MockMultipartFile("arquivo", "encaminhamento.pdf", "application/pdf", "x".getBytes());

        mockMvc.perform(multipart("/pre-guias")
                        .file(arquivo)
                        .param("cpf", "12345678901")
                        .param("precCp", "0001112223"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes[0]").value(org.hamcrest.Matchers.containsString("ocsId")));
    }
}