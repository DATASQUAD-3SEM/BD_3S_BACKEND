package fatec.fusex.nexus.usuario;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;

@WebMvcTest(UsuarioAssinaturaController.class)
@Import(SimulacaoLogin.class)
class UsuarioAssinaturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioAssinaturaService assinaturaService;

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
}