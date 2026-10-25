package fatec.fusex.nexus.setorGuias;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class SetorGuiasEncaminhamentoTest {

    private static final String HEADER = "X-Usuario-Login";
    private static final String SETOR = "setor.guias";
    private static final String BENEFICIARIO_LOGIN = "beneficiario.teste";

    private final MockMvc mockMvc;
    private final PreGuiaRepository preGuiaRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final MedicoFusexRepository medicoFusexRepository;
    private final OcsRepository ocsRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    private Ocs ocs;
    private Beneficiario beneficiario;
    private MedicoFusex medico;

    SetorGuiasEncaminhamentoTest(
            MockMvc mockMvc,
            PreGuiaRepository preGuiaRepository,
            BeneficiarioRepository beneficiarioRepository,
            MedicoFusexRepository medicoFusexRepository,
            OcsRepository ocsRepository,
            UsuarioRepository usuarioRepository,
            ArmazenamentoArquivo armazenamentoArquivo) {
        this.mockMvc = mockMvc;
        this.preGuiaRepository = preGuiaRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.medicoFusexRepository = medicoFusexRepository;
        this.ocsRepository = ocsRepository;
        this.usuarioRepository = usuarioRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    @BeforeEach
    void setUp() {
        novoUsuario(SETOR, PerfilUsuario.SETOR_GUIAS);
        novoUsuario(BENEFICIARIO_LOGIN, PerfilUsuario.BENEFICIARIO);

        ocs = new Ocs();
        ocs.setNome("Laboratorio Exemplo");
        ocs.setContratoNum("CTR-900");
        ocs.setTipo(OcsTipo.OCS);
        ocs = ocsRepository.save(ocs);

        Usuario uBenef = novoUsuario("52998224725", PerfilUsuario.BENEFICIARIO);
        beneficiario = new Beneficiario();
        beneficiario.setUsuario(uBenef);
        beneficiario.setNome("Marta Souza Lima");
        beneficiario.setPrecCp("45810020001");
        beneficiario.setDataNascimento(LocalDate.of(1990, 1, 1));
        beneficiario.setTelefone("11987654321");
        beneficiario = beneficiarioRepository.save(beneficiario);

        Usuario uMedico = novoUsuario("medico.teste", PerfilUsuario.MEDICO);
        medico = new MedicoFusex();
        medico.setUsuario(uMedico);
        medico.setNome("Cap Med Ribeiro");
        medico.setPrecCp("11122233344");
        medico.setPatente("Capitao");
        medico.setCrm("CRM-TESTE-1");
        medico = medicoFusexRepository.save(medico);
    }

    @Test
    @DisplayName("imagem PNG: devolve image/png inline com nome encaminhamento-{id}.png")
    void imagem() throws Exception {
        String caminho = salvarEncaminhamento("exame.png", "fake-png".getBytes());
        PreGuia pg = criarPreGuia(null, PreGuiaStatus.PENDENTE, caminho);

        mockMvc.perform(get(url(pg)).header(HEADER, SETOR))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(header().string("Content-Disposition",
                        containsString("encaminhamento-" + numero(pg) + ".png")))
                .andExpect(content().bytes("fake-png".getBytes()));
    }

    @Test
    @DisplayName("PDF: devolve application/pdf inline")
    void pdf() throws Exception {
        String caminho = salvarEncaminhamento("exame.pdf", "%PDF-1.4 fake".getBytes());
        PreGuia pg = criarPreGuia(null, PreGuiaStatus.PENDENTE, caminho);

        mockMvc.perform(get(url(pg)).header(HEADER, SETOR))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("inline")))
                .andExpect(content().bytes("%PDF-1.4 fake".getBytes()));
    }

    @Test
    @DisplayName("origem MEDICO: 404, pois nao ha encaminhamento")
    void origemMedico() throws Exception {
        PreGuia pg = criarPreGuia(medico, PreGuiaStatus.EM_ANALISE, null);

        mockMvc.perform(get(url(pg)).header(HEADER, SETOR))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("pre-guia inexistente: 404")
    void naoExiste() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias/999999/encaminhamento").header(HEADER, SETOR))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("perfil diferente de SETOR_GUIAS: 403")
    void perfilErrado() throws Exception {
        String caminho = salvarEncaminhamento("exame.pdf", "%PDF-1.4 fake".getBytes());
        PreGuia pg = criarPreGuia(null, PreGuiaStatus.PENDENTE, caminho);

        mockMvc.perform(get(url(pg)).header(HEADER, BENEFICIARIO_LOGIN))
                .andExpect(status().isForbidden());
    }

    // ---------- auxiliares ----------

    private String url(PreGuia pg) {
        return "/setor-guias/pre-guias/" + pg.getIdPreGuia() + "/encaminhamento";
    }

    private String numero(PreGuia pg) {
        return String.format("%04d", pg.getIdPreGuia());
    }

    private Usuario novoUsuario(String login, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash("hash");
        u.setPerfil(perfil);
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }

    private PreGuia criarPreGuia(MedicoFusex criador, PreGuiaStatus status, String encaminhamentoUrl) {
        PreGuia pg = new PreGuia();
        pg.setBeneficiario(beneficiario);
        pg.setOcs(ocs);
        pg.setMedicoFusex(criador);
        pg.setStatus(status);
        pg.setEncaminhamentoUrl(encaminhamentoUrl);
        pg.setDataCriacao(LocalDateTime.now());
        return preGuiaRepository.save(pg);
    }

    private String salvarEncaminhamento(String nome, byte[] bytes) {
        String tipo = nome.endsWith(".png") ? "image/png" : "application/pdf";
        return armazenamentoArquivo.salvar(
                new MockMultipartFile("arquivo", nome, tipo, bytes),
                "encaminhamentos");
    }
}