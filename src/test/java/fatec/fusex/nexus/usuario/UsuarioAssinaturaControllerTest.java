package fatec.fusex.nexus.usuario;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;

/**
 * Testes HTTP de PUT /usuarios/me/assinatura (T13-BE2) com arquivo e com imagem em base64
 * e de GET /usuarios/me/assinatura (T37-BE3).
 */
@WebMvcTest(UsuarioAssinaturaController.class)
@Import(SimulacaoLogin.class)
class UsuarioAssinaturaControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private UsuarioAssinaturaService assinaturaService;

    UsuarioAssinaturaControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void atualizarArquivo_quandoSucesso_devolve200() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo",
                "assinatura.png",
                "image/png",
                new byte[] {1, 2, 3});

        mockMvc.perform(
                        multipart("/usuarios/me/assinatura")
                                .file(arquivo)
                                .header(
                                        SimulacaoLoginFilter.HEADER,
                                        "usuario-teste")
                                .with(request -> {
                                    request.setMethod("PUT");
                                    return request;
                                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assinaturaCadastrada").value(true));
    }

    @Test
    void atualizarBase64_quandoSucesso_devolve200() throws Exception {
        mockMvc.perform(
                        put("/usuarios/me/assinatura")
                                .header(
                                        SimulacaoLoginFilter.HEADER,
                                        "usuario-teste")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "imagemBase64": "aW1hZ2VtLXRlc3Rl"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assinaturaCadastrada").value(true));
    }

    @Test
    void atualizarBase64_quandoImagemInvalida_devolve422()
            throws Exception {

        doThrow(new DadosInvalidosException(
                "assinatura", "Imagem Base64 inválida"))
                .when(assinaturaService)
                .atualizarComBase64(anyString());

        mockMvc.perform(
                        put("/usuarios/me/assinatura")
                                .header(
                                        SimulacaoLoginFilter.HEADER,
                                        "usuario-teste")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "imagemBase64": "conteudo-invalido"
                                        }
                                        """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]")
                        .value("assinatura: Imagem Base64 inválida"));
    }

    @Test
    void buscarAtual_quandoTemAssinatura_devolvePngSemCache()
            throws Exception {

        byte[] png = {(byte) 0x89, 'P', 'N', 'G'};

        when(assinaturaService.buscarImagemAtual()).thenReturn(png);

        mockMvc.perform(
                        get("/usuarios/me/assinatura")
                                .header(
                                        SimulacaoLoginFilter.HEADER,
                                        "usuario-teste"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(png));
    }

    @Test
    void buscarAtual_quandoSemAssinatura_devolve404() throws Exception {
        when(assinaturaService.buscarImagemAtual())
                .thenThrow(new RecursoNaoEncontradoException(
                        "Assinatura não cadastrada."));

        mockMvc.perform(
                        get("/usuarios/me/assinatura")
                                .header(
                                        SimulacaoLoginFilter.HEADER,
                                        "usuario-teste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem")
                        .value("Assinatura não cadastrada."));
    }

    @Test
    void buscarAtual_quandoContaDeSetor_devolve403() throws Exception {
        when(assinaturaService.buscarImagemAtual())
                .thenThrow(new AcessoNegadoException(
                        "Este perfil nao tem assinatura"));

        mockMvc.perform(
                        get("/usuarios/me/assinatura")
                                .header(
                                        SimulacaoLoginFilter.HEADER,
                                        "setor-guias"))
                .andExpect(status().isForbidden());
    }
}