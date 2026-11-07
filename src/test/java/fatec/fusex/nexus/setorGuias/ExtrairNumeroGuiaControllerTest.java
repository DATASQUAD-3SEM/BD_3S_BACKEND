package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.guiaAssinatura.PdfsDeTeste;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** Ponta a ponta (filtro de login + controller + service) do POST /setor-guias/extrair-numero-guia (T11-BE1). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ExtrairNumeroGuiaControllerTest {

    private static final String ROTA = "/setor-guias/extrair-numero-guia";
    private static final String SETOR = "setor.leitura";
    private static final String BENEFICIARIO = "52998224725";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final GuiaRascunhoRepository guiaRascunhoRepository;

    ExtrairNumeroGuiaControllerTest(MockMvc mockMvc,
                                    UsuarioRepository usuarioRepository,
                                    GuiaRascunhoRepository guiaRascunhoRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.guiaRascunhoRepository = guiaRascunhoRepository;
    }

    @BeforeEach
    void usuarios() {
        usuario(SETOR, PerfilUsuario.SETOR_GUIAS);
        usuario(BENEFICIARIO, PerfilUsuario.BENEFICIARIO);
    }

    private void usuario(String login, PerfilUsuario perfil) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenhaHash("hash");
        usuario.setPerfil(perfil);
        usuario.setAtivo(true);
        usuarioRepository.saveAndFlush(usuario);
    }

    private ResultActions ler(String login, MockMultipartFile arquivo) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart(HttpMethod.POST, ROTA);
        if (arquivo != null) {
            req.file(arquivo);
        }
        return mockMvc.perform(req.header(SimulacaoLoginFilter.HEADER, login));
    }

    private static MockMultipartFile pdf(String nome, byte[] conteudo) {
        return new MockMultipartFile("pdf", nome, "application/pdf", conteudo);
    }

    @Test
    void pdfComNumero_retorna200ComONumeroESemGravarNada() throws Exception {
        long rascunhosAntes = guiaRascunhoRepository.count();
        byte[] conteudo = PdfsDeTeste.pdfComTexto("GUIA DE ENCAMINHAMENTO", "Nr: 202610678");

        ler(SETOR, pdf("guia-sire.pdf", conteudo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("202610678"));

        assertThat(guiaRascunhoRepository.count()).isEqualTo(rascunhosAntes);
    }

    @Test
    void pdfSemPadrao_retorna422ComMensagemDeLeitura() throws Exception {
        byte[] conteudo = PdfsDeTeste.pdfComTexto("GUIA DE ENCAMINHAMENTO", "Sem numero neste texto");

        ler(SETOR, pdf("sem-numero.pdf", conteudo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detalhes[0]").value(
                        "pdf: Nao foi possivel ler o numero da Guia neste PDF. Digite o numero manualmente."));
    }

    @Test
    void pdfEscaneadoSemTexto_retorna422() throws Exception {
        byte[] conteudo = PdfsDeTeste.pdfComTexto();

        ler(SETOR, pdf("escaneado.pdf", conteudo)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void jpgOuPng_retorna400() throws Exception {
        byte[] jpg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        byte[] png = new byte[] {(byte) 0x89, 'P', 'N', 'G'};

        ler(SETOR, new MockMultipartFile("pdf", "foto.jpg", "image/jpeg", jpg)).andExpect(status().isBadRequest());
        ler(SETOR, new MockMultipartFile("pdf", "foto.png", "image/png", png)).andExpect(status().isBadRequest());
    }

    @Test
    void semArquivo_retorna400() throws Exception {
        ler(SETOR, null).andExpect(status().isBadRequest());
    }

    @Test
    void acimaDe10Mb_retorna400() throws Exception {
        byte[] grande = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy("%PDF".getBytes(), 0, grande, 0, 4);

        ler(SETOR, pdf("grande.pdf", grande))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O PDF deve ter no maximo 10 MB"));
    }

    @Test
    void perfilSemPermissao_retorna403() throws Exception {
        byte[] conteudo = PdfsDeTeste.pdfComTexto("GUIA DE ENCAMINHAMENTO", "Nr: 202610678");

        ler(BENEFICIARIO, pdf("guia.pdf", conteudo)).andExpect(status().isForbidden());
    }

    @Test
    void excessoDeRequisicoes_retorna429() throws Exception {
        usuario("setor.limite", PerfilUsuario.SETOR_GUIAS);

        // Sem arquivo, cada chamada falha com 400, mas ainda conta no limite de 20 por minuto
        for (int i = 0; i < 20; i++) {
            ler("setor.limite", null).andExpect(status().isBadRequest());
        }
        ler("setor.limite", null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }
}
