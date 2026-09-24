package fatec.fusex.nexus.preguia;

import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EncaminhamentoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ArmazenamentoArquivo armazenamentoArquivo;

    @InjectMocks
    private EncaminhamentoController encaminhamentoController;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(encaminhamentoController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Deve retornar Status 201 e caminho do arquivo quando o upload for valido (PDF)")
    void deveFazerUploadComSucessoQuandoArquivoForValido() throws Exception {
        MockMultipartFile arquivoValido = new MockMultipartFile(
                "file",
                "encaminhamento.pdf",
                "application/pdf",
                "Conteudo do arquivo PDF".getBytes()
        );

        when(armazenamentoArquivo.salvar(any(), eq("encaminhamentos")))
                .thenReturn("encaminhamentos/uuid_encaminhamento.pdf");

        mockMvc.perform(multipart("/encaminhamentos").file(arquivoValido))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.encaminhamentoUrl").value("encaminhamentos/uuid_encaminhamento.pdf"));
    }

    @Test
    @DisplayName("Deve retornar Status 400 Bad Request quando o tipo de arquivo for invalido (ex: .txt)")
    void deveRetornarErroQuandoTipoDeArquivoForInvalido() throws Exception {
        MockMultipartFile arquivoInvalido = new MockMultipartFile(
                "file",
                "documento.txt",
                "text/plain",
                "Conteudo de texto simples".getBytes()
        );

        mockMvc.perform(multipart("/encaminhamentos").file(arquivoInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Tipo de arquivo inválido. Apenas PDF, JPG e PNG são permitidos."));
    }

    @Test
    @DisplayName("Deve retornar Status 400 Bad Request quando o arquivo exceder o tamanho maximo de 10MB")
    void deveRetornarErroQuandoTamanhoExcederLimite() throws Exception {
        byte[] conteudoGrande = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile arquivoGrande = new MockMultipartFile(
                "file",
                "imagem_grande.jpg",
                "image/jpeg",
                conteudoGrande
        );

        mockMvc.perform(multipart("/encaminhamentos").file(arquivoGrande))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O arquivo excede o tamanho máximo permitido de 10MB."));
    }

    @Test
    @DisplayName("Deve retornar Status 400 Bad Request quando nenhum conteudo de arquivo for enviado")
    void deveRetornarErroQuandoArquivoEstiverVazio() throws Exception {
        MockMultipartFile arquivoVazio = new MockMultipartFile(
                "file",
                "vazio.pdf",
                "application/pdf",
                new byte[0]
        );

        mockMvc.perform(multipart("/encaminhamentos").file(arquivoVazio))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O arquivo de encaminhamento não foi enviado."));
    }
}