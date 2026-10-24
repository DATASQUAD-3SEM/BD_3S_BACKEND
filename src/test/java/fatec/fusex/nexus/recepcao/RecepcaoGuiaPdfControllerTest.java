package fatec.fusex.nexus.recepcao;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;

class RecepcaoGuiaPdfControllerTest {

    private static final byte[] CONTEUDO = "%PDF-1.4 conteudo".getBytes();

    private MockMvc mockMvc;

    @Mock
    private RecepcaoGuiaPdfService recepcaoGuiaPdfService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RecepcaoGuiaPdfController(recepcaoGuiaPdfService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void visualizar_devolvePdfInline() throws Exception {
        when(recepcaoGuiaPdfService.pdfDaGuia(1L, "visualizar"))
                .thenReturn(new PdfGuiaRecepcao(new ByteArrayResource(CONTEUDO), "guia-00042.pdf", false));

        mockMvc.perform(get("/recepcao/guias/1/pdf").param("modo", "visualizar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"guia-00042.pdf\""))
                .andExpect(content().bytes(CONTEUDO));
    }

    @Test
    void imprimir_devolvePdfInline() throws Exception {
        when(recepcaoGuiaPdfService.pdfDaGuia(1L, "imprimir"))
                .thenReturn(new PdfGuiaRecepcao(new ByteArrayResource(CONTEUDO), "guia-00042.pdf", false));

        mockMvc.perform(get("/recepcao/guias/1/pdf").param("modo", "imprimir"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"guia-00042.pdf\""));
    }

    @Test
    void baixar_devolvePdfComoAttachment() throws Exception {
        when(recepcaoGuiaPdfService.pdfDaGuia(1L, "baixar"))
                .thenReturn(new PdfGuiaRecepcao(new ByteArrayResource(CONTEUDO), "guia-00042.pdf", true));

        mockMvc.perform(get("/recepcao/guias/1/pdf").param("modo", "baixar"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guia-00042.pdf\""))
                .andExpect(content().bytes(CONTEUDO));
    }

    @Test
    void modoAusente_devolve400NoFormatoPadrao() throws Exception {
        when(recepcaoGuiaPdfService.pdfDaGuia(1L, null))
                .thenThrow(new RegraDeNegocioException("Informe o modo: visualizar, imprimir ou baixar"));

        mockMvc.perform(get("/recepcao/guias/1/pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void guiaNaoConcluida_devolve403() throws Exception {
        when(recepcaoGuiaPdfService.pdfDaGuia(1L, "visualizar"))
                .thenThrow(new AcessoNegadoException("Guia indisponivel"));

        mockMvc.perform(get("/recepcao/guias/1/pdf").param("modo", "visualizar"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void guiaInexistente_devolve404() throws Exception {
        when(recepcaoGuiaPdfService.pdfDaGuia(99L, "visualizar"))
                .thenThrow(new RecursoNaoEncontradoException("Guia nao encontrada: id 99"));

        mockMvc.perform(get("/recepcao/guias/99/pdf").param("modo", "visualizar"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void perfilErrado_devolve403() throws Exception {
        when(recepcaoGuiaPdfService.pdfDaGuia(1L, "visualizar"))
                .thenThrow(new AcessoNegadoException("Acesso restrito a recepcao"));

        mockMvc.perform(get("/recepcao/guias/1/pdf").param("modo", "visualizar"))
                .andExpect(status().isForbidden());
    }
}