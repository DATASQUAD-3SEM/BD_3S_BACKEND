package fatec.fusex.nexus.minhasPreGuias;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de GET /beneficiarios/me/pre-guias/{id}/encaminhamento/arquivo (T9-BE2): imagem, PDF, outro beneficiario e origem medico.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class MinhasPreGuiasEncaminhamentoControllerTest {

    private static final String URL = "/beneficiarios/me/pre-guias/{id}/encaminhamento/arquivo";
    private static final String HEADER = SimulacaoLoginFilter.HEADER;
    private static final String LOGIN_JOAO = "11111111111";
    private static final String LOGIN_MARIA = "22222222222";

    private static final LocalDateTime CRIACAO = LocalDateTime.of(2026, 10, 10, 9, 0);
    private static final byte[] PNG_BYTES = "fake-png-conteudo".getBytes(StandardCharsets.UTF_8);
    private static final byte[] JPG_BYTES = "fake-jpg-conteudo".getBytes(StandardCharsets.UTF_8);
    private static final byte[] PDF_BYTES = "%PDF-1.4\nfake\n%%EOF".getBytes(StandardCharsets.UTF_8);

    private final MockMvc mockMvc;
    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedicoFusexRepository medicoFusexRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    private Beneficiario joao;
    private Beneficiario maria;
    private Ocs ocsPadrao;
    private MedicoFusex medico;

    MinhasPreGuiasEncaminhamentoControllerTest(
            MockMvc mockMvc,
            PreGuiaRepository preGuiaRepository,
            BeneficiarioRepository beneficiarioRepository,
            OcsRepository ocsRepository,
            UsuarioRepository usuarioRepository,
            MedicoFusexRepository medicoFusexRepository,
            ArmazenamentoArquivo armazenamentoArquivo) {
        this.mockMvc = mockMvc;
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.usuarioRepository = usuarioRepository;
        this.medicoFusexRepository = medicoFusexRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    @BeforeEach
    void setUp() {
        Usuario usuarioJoao = criarUsuario(LOGIN_JOAO, PerfilUsuario.BENEFICIARIO);
        joao = criarBeneficiario(usuarioJoao, "João Logado", LOGIN_JOAO);

        Usuario usuarioMaria = criarUsuario(LOGIN_MARIA, PerfilUsuario.BENEFICIARIO);
        maria = criarBeneficiario(usuarioMaria, "Maria Outra", LOGIN_MARIA);

        Usuario usuarioMedico = criarUsuario("33333333333", PerfilUsuario.MEDICO);
        medico = new MedicoFusex();
        medico.setUsuario(usuarioMedico);
        medico.setNome("Dr. Carlos");
        medico.setPrecCp("33333333333");
        medico.setPatente("Capitão");
        medico.setCrm("123456-SP");
        medico = medicoFusexRepository.save(medico);

        ocsPadrao = new Ocs();
        ocsPadrao.setNome("Hospital Central");
        ocsPadrao.setContratoNum("123456789");
        ocsPadrao.setTipo(OcsTipo.OCS);
        ocsPadrao = ocsRepository.save(ocsPadrao);
    }

    @Test
    @DisplayName("imagem PNG: devolve arquivo inline com Content-Type image/png")
    void imagemPng_retornaInline() throws Exception {
        String url = salvarArquivo("foto.png", "image/png", PNG_BYTES);
        PreGuia pg = criarPreGuia(joao, null, url);

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("inline")))
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    @DisplayName("imagem JPG: devolve arquivo inline com Content-Type image/jpeg")
    void imagemJpg_retornaInline() throws Exception {
        String url = salvarArquivo("foto.jpg", "image/jpeg", JPG_BYTES);
        PreGuia pg = criarPreGuia(joao, null, url);

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("inline")))
                .andExpect(content().bytes(JPG_BYTES));
    }

    @Test
    @DisplayName("PDF: devolve arquivo inline com Content-Type application/pdf")
    void pdf_retornaInline() throws Exception {
        String url = salvarArquivo("encaminhamento.pdf", "application/pdf", PDF_BYTES);
        PreGuia pg = criarPreGuia(joao, null, url);

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("inline")))
                .andExpect(content().bytes(PDF_BYTES));
    }

    @Test
    @DisplayName("404: pre-guia de outro beneficiario")
    void preGuiaDeOutroBeneficiario_retorna404() throws Exception {
        String url = salvarArquivo("foto.png", "image/png", PNG_BYTES);
        PreGuia pg = criarPreGuia(maria, null, url);

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("404: pre-guia inexistente")
    void preGuiaInexistente_retorna404() throws Exception {
        mockMvc.perform(get(URL, 99999L).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("404: pre-guia sem anexo de encaminhamento")
    void semAnexo_retorna404() throws Exception {
        PreGuia pg = criarPreGuia(joao, null, null);

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("403: pre-guia criada por medico")
    void origemMedico_retorna403() throws Exception {
        String url = salvarArquivo("foto.png", "image/png", PNG_BYTES);
        PreGuia pg = criarPreGuia(joao, medico, url);

        mockMvc.perform(get(URL, pg.getIdPreGuia()).header(HEADER, LOGIN_JOAO))
                .andExpect(status().isForbidden());
    }

    // ===== helpers =====

    private Usuario criarUsuario(String login, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash("hash");
        u.setPerfil(perfil);
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }

    private Beneficiario criarBeneficiario(Usuario usuario, String nome, String precCp) {
        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome(nome);
        b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1990, 1, 1));
        b.setTelefone("11999999991");
        return beneficiarioRepository.save(b);
    }

    private PreGuia criarPreGuia(Beneficiario dono, MedicoFusex medicoCriador, String encaminhamentoUrl) {
        PreGuia pg = new PreGuia();
        pg.setBeneficiario(dono);
        pg.setOcs(ocsPadrao);
        pg.setStatus(PreGuiaStatus.ATENCAO);
        pg.setDataCriacao(CRIACAO);
        pg.setEncaminhamentoUrl(encaminhamentoUrl);
        pg.setMedicoFusex(medicoCriador);
        return preGuiaRepository.save(pg);
    }

    private String salvarArquivo(String nome, String contentType, byte[] bytes) {
        MockMultipartFile f = new MockMultipartFile("arquivo", nome, contentType, bytes);
        return armazenamentoArquivo.salvar(f, "encaminhamentos");
    }
}