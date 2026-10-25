package fatec.fusex.nexus.guiaAssinatura;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import fatec.fusex.nexus.common.exception.ConflitoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.guiaAssinatura.dto.AssinaturaRegistradaResponse;
import fatec.fusex.nexus.guiaAssinatura.dto.GuiaParaAssinarResponse;
import fatec.fusex.nexus.guiaAssinatura.dto.PdfGuiaAssinada;

/**
 * Testes HTTP dos endpoints de Guias para assinar (T5-BE1, T5-BE2 e T5-BE3), com os servicos simulados.
 */
class GuiaAssinaturaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GuiaAssinaturaService guiaAssinaturaService;

    @Mock
    private GuiaPdfAssinadoService guiaPdfAssinadoService;

    @Mock
    private RegistrarAssinaturaGuiaService registrarAssinaturaGuiaService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GuiaAssinaturaController(
                        guiaAssinaturaService, guiaPdfAssinadoService, registrarAssinaturaGuiaService))
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
                .andExpect(jsonPath("$[0].pendencias[0]").value("MEDICO"))
                .andExpect(jsonPath("$[0].pendencias[1]").value("CHEFE"));
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
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment")));
    }

    @Test
    void pdf_guiaInexistente_devolve404() throws Exception {
        when(guiaPdfAssinadoService.pdfDaGuia(9L, "visualizar"))
                .thenThrow(new RecursoNaoEncontradoException("Guia nao encontrada: id 9"));

        mockMvc.perform(get("/guias/9/pdf").param("modo", "visualizar"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pdf_modoInvalido_devolve422() throws Exception {
        when(guiaPdfAssinadoService.pdfDaGuia(1L, "imprimir"))
                .thenThrow(new DadosInvalidosException("modo", "Modo invalido: use baixar ou visualizar"));

        mockMvc.perform(get("/guias/1/pdf").param("modo", "imprimir"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void assinar_devolve201ComAssinaturasEConcluida() throws Exception {
        when(registrarAssinaturaGuiaService.assinar(1L)).thenReturn(
                new AssinaturaRegistradaResponse(
                        List.of(AssinaturaGuiaPapel.MEDICO, AssinaturaGuiaPapel.CHEFE), true));

        mockMvc.perform(post("/guias/1/assinaturas"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assinaturas[0]").value("MEDICO"))
                .andExpect(jsonPath("$.assinaturas[1]").value("CHEFE"))
                .andExpect(jsonPath("$.concluida").value(true));
    }

    @Test
    void assinar_papelJaAssinado_devolve409() throws Exception {
        when(registrarAssinaturaGuiaService.assinar(1L))
                .thenThrow(new ConflitoException("Esta Guia ja foi assinada por um medico do FUSEX"));

        mockMvc.perform(post("/guias/1/assinaturas"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Esta Guia ja foi assinada por um medico do FUSEX"));
    }

    @Test
    void assinar_semAssinaturaCadastrada_devolve422() throws Exception {
        when(registrarAssinaturaGuiaService.assinar(1L))
                .thenThrow(new DadosInvalidosException("assinatura",
                        "Cadastre sua assinatura em Minhas informacoes antes de assinar"));

        mockMvc.perform(post("/guias/1/assinaturas"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void assinar_usuarioInativoOuPerfilErrado_devolve403() throws Exception {
        when(registrarAssinaturaGuiaService.assinar(1L))
                .thenThrow(new AcessoNegadoException("Usuario inativo: nao pode assinar Guias"));

        mockMvc.perform(post("/guias/1/assinaturas"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assinar_guiaInexistente_devolve404() throws Exception {
        when(registrarAssinaturaGuiaService.assinar(9L))
                .thenThrow(new RecursoNaoEncontradoException("Guia nao encontrada: id 9"));

        mockMvc.perform(post("/guias/9/assinaturas"))
                .andExpect(status().isNotFound());
    }
}