package fatec.fusex.nexus.rastreabilidade;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import fatec.fusex.nexus.setorFatura.PdfGuiaFatura;
import fatec.fusex.nexus.setorFatura.PdfGuiaFaturaService;
import fatec.fusex.nexus.usuario.PerfilUsuario;

class RastreabilidadeGuiaPdfControllerTest {

    private static final byte[] CONTEUDO = "%PDF-1.4 conteudo".getBytes();

    private MockMvc mockMvc;

    @Mock
    private PdfGuiaFaturaService pdfGuiaFaturaService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new RastreabilidadeGuiaPdfController(pdfGuiaFaturaService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void guiaDeFaturaEmLisura_devolvePdfInline() throws Exception {
        when(pdfGuiaFaturaService.pdfDaGuia(1L, PerfilUsuario.RASTREABILIDADE))
                .thenReturn(new PdfGuiaFatura(new ByteArrayResource(CONTEUDO), "guia-00042.pdf"));

        mockMvc.perform(get("/rastreabilidade/guias/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"guia-00042.pdf\""))
                .andExpect(content().bytes(CONTEUDO));
    }

    @Test
    void guiaForaDasFaturasDoSetor_devolve404() throws Exception {
        when(pdfGuiaFaturaService.pdfDaGuia(2L, PerfilUsuario.RASTREABILIDADE))
                .thenThrow(new RecursoNaoEncontradoException("Guia nao encontrada nas faturas do setor: id 2"));

        mockMvc.perform(get("/rastreabilidade/guias/2/pdf"))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilErrado_devolve403() throws Exception {
        when(pdfGuiaFaturaService.pdfDaGuia(1L, PerfilUsuario.RASTREABILIDADE))
                .thenThrow(new AcessoNegadoException("Acesso negado: este endpoint e exclusivo do perfil RASTREABILIDADE"));

        mockMvc.perform(get("/rastreabilidade/guias/1/pdf"))
                .andExpect(status().isForbidden());
    }
}