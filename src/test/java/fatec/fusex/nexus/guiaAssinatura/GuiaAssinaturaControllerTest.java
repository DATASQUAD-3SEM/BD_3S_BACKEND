package fatec.fusex.nexus.guiaAssinatura;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guiaAssinatura.dto.GuiaParaAssinarResponse;
import fatec.fusex.nexus.guiaAssinatura.dto.PdfGuiaAssinada;

class GuiaAssinaturaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GuiaAssinaturaService guiaAssinaturaService;

    @Mock
    private GuiaPdfAssinadoService guiaPdfAssinadoService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GuiaAssinaturaController(guiaAssinaturaService, guiaPdfAssinadoService))
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

    @Test
    void pdf_baixar_devolve200ComAnexo() throws Exception {
        when(guiaPdfAssinadoService.pdfDaGuia(1L, "baixar")).thenReturn(new PdfGuiaAssinada(
                new ByteArrayResource("%PDF-1.4".getBytes()), "guia-202610678.pdf", true));

        mockMvc.perform(get("/guias/1/pdf").param("modo", "baixar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("guia-202610678.pdf")));
    }

    @Test
    void pdf_visualizar_devolve200ComInline() throws Exception {
        when(guiaPdfAssinadoService.pdfDaGuia(1L, "visualizar")).thenReturn(new PdfGuiaAssinada(
                new ByteArrayResource("%PDF-1.4".getBytes()), "guia-202610678.pdf", false));

        mockMvc.perform(get("/guias/1/pdf").param("modo", "visualizar"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")));
    }

    @Test
    void pdf_guiaInexistente_devolve404() throws Exception {
        when(guiaPdfAssinadoService.pdfDaGuia(9L, "visualizar"))
                .thenThrow(new RecursoNaoEncontradoException("Guia nao encontrada: id 9"));

        mockMvc.perform(get("/guias/9/pdf").param("modo", "visualizar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pdf_perfilSemPermissao_devolve403() throws Exception {
        when(guiaPdfAssinadoService.pdfDaGuia(1L, "visualizar"))
                .thenThrow(new AcessoNegadoException("Acesso restrito a medicos e chefe do FUSEX"));

        mockMvc.perform(get("/guias/1/pdf").param("modo", "visualizar"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pdf_modoInvalido_devolve422() throws Exception {
        when(guiaPdfAssinadoService.pdfDaGuia(1L, "imprimir"))
                .thenThrow(new DadosInvalidosException("modo", "Modo invalido: use baixar ou visualizar"));

        mockMvc.perform(get("/guias/1/pdf").param("modo", "imprimir"))
                .andExpect(status().isUnprocessableEntity());
    }
}