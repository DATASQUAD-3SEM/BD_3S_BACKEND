package fatec.fusex.nexus.guiaAssinatura;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinatura.AssinaturaRepository;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

/**
 * T23-BE2: GET /guias/{id}/pdf para o beneficiario dono da Guia (inline, sem modo, no-store), com as regras de
 * entrega da T1-BE2. O medico segue com modo obrigatorio, como antes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GuiaPdfBeneficiarioTest {

    // Mesma raiz do nexus.storage.local-dir do perfil test (application-test.properties).
    private static final Path RAIZ_ARQUIVOS = Path.of("target", "uploads-test");
    private static final AtomicInteger ARQUIVOS = new AtomicInteger();
    private static final String LOGIN_JOAO = "11111111111";
    private static final String LOGIN_MARIA = "22222222222";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AssinaturaGuiaRepository assinaturaGuiaRepository;

    private Usuario usuarioJoao;
    private Beneficiario joao;
    private Usuario maria;
    private Usuario setor;
    private Usuario medico;
    private Ocs ocs;
    private int sequencia;

    GuiaPdfBeneficiarioTest(MockMvc mockMvc,
                            UsuarioRepository usuarioRepository,
                            BeneficiarioRepository beneficiarioRepository,
                            OcsRepository ocsRepository,
                            PreGuiaRepository preGuiaRepository,
                            GuiaRepository guiaRepository,
                            AssinaturaRepository assinaturaRepository,
                            AssinaturaGuiaRepository assinaturaGuiaRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
    }

    @BeforeEach
    void setUp() {
        ocs = new Ocs();
        ocs.setNome("Laboratorio Lab");
        ocs.setContratoNum("CTR-LAB-PDF");
        ocs.setTipo(OcsTipo.OCS);
        ocs = ocsRepository.save(ocs);

        usuarioJoao = usuario(LOGIN_JOAO, PerfilUsuario.BENEFICIARIO);
        joao = beneficiario(usuarioJoao, LOGIN_JOAO, "Joao Logado");
        maria = usuario(LOGIN_MARIA, PerfilUsuario.BENEFICIARIO);
        beneficiario(maria, LOGIN_MARIA, "Maria Outra");
        setor = usuario("setor.pdf", PerfilUsuario.SETOR_GUIAS);
        medico = usuario("medico.pdf", PerfilUsuario.MEDICO);
    }

    @Test
    void donoDaGuiaConcluida_recebePdfInline_semModo_eSemCache() throws Exception {
        Guia guia = guiaComDuasAssinaturas(PreGuiaStatus.CONCLUIDA, "guias/ok.pdf", true);

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_JOAO))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", startsWith("inline")))
                .andExpect(header().string("Content-Disposition", containsString("guia-")))
                .andExpect(header().string("Cache-Control", containsString("no-store")));
    }

    @Test
    void guiaDeOutroBeneficiario_recebe403() throws Exception {
        Guia guia = guiaComDuasAssinaturas(PreGuiaStatus.CONCLUIDA, "guias/outro.pdf", true);

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_MARIA))
                .andExpect(status().isForbidden());
    }

    @Test
    void preGuiaNaoConcluida_recebe403() throws Exception {
        Guia guia = guiaComDuasAssinaturas(PreGuiaStatus.PENDENTE, "guias/pendente.pdf", true);

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_JOAO))
                .andExpect(status().isForbidden());
    }

    @Test
    void guiaComUmaAssinatura_recebe403() throws Exception {
        Guia guia = guiaComDuasAssinaturas(PreGuiaStatus.CONCLUIDA, "guias/uma.pdf", false);

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_JOAO))
                .andExpect(status().isForbidden());
    }

    @Test
    void arquivoGuardadoAusente_recebe404() throws Exception {
        Guia guia = guiaComDuasAssinaturas(PreGuiaStatus.CONCLUIDA, "guias/ausente.pdf", true);
        Files.delete(RAIZ_ARQUIVOS.resolve(guia.getArquivoUrl()));

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_JOAO))
                .andExpect(status().isNotFound());
    }
    
    @Test
    void guiaInexistente_recebe404() throws Exception {
        mockMvc.perform(get("/guias/999999/pdf").header(SimulacaoLoginFilter.HEADER, LOGIN_JOAO))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilOcs_recebe403() throws Exception {
        Guia guia = guiaComDuasAssinaturas(PreGuiaStatus.CONCLUIDA, "guias/ocs.pdf", true);
        usuario("ocs.pdf", PerfilUsuario.OCS);

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, "ocs.pdf"))
                .andExpect(status().isForbidden());
    }

    @Test
    void medicoSemModo_continuaRecebendo422() throws Exception {
        Guia guia = guiaComDuasAssinaturas(PreGuiaStatus.CONCLUIDA, "guias/medico.pdf", true);

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, "medico.pdf"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void medicoComModoBaixar_continuaRecebendoAnexo() throws Exception {
        Guia guia = guiaComDuasAssinaturas(PreGuiaStatus.CONCLUIDA, "guias/medico-baixar.pdf", true);

        mockMvc.perform(get(url(guia)).param("modo", "baixar")
                        .header(SimulacaoLoginFilter.HEADER, "medico.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", startsWith("attachment")));
    }

    // ---------- fixtures ----------

    private static String url(Guia guia) {
        return "/guias/" + guia.getIdGuia() + "/pdf";
    }

    /**
     * Guia da pre-guia de Joao. Com {@code duasAssinaturas}, grava as assinaturas do medico e do chefe; senao,
     * so a do medico. O PDF e gravado em disco no caminho guardado, exceto nos casos de arquivo ausente.
     */
    private Guia guiaComDuasAssinaturas(PreGuiaStatus status, String caminho, boolean duasAssinaturas)
            throws IOException {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(status);
        preGuia.setBeneficiario(joao);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamento.pdf");
        preGuia.setDataCriacao(LocalDateTime.of(2026, 10, 1, 9, 0));
        preGuia = preGuiaRepository.save(preGuia);

        String caminhoUnico = caminho.replace(".pdf", "-" + ARQUIVOS.incrementAndGet() + ".pdf");
        gravar(caminhoUnico, PdfsDeTeste.pdfComRotulos(true, true));

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(String.format("%09d", ++sequencia));
        guia.setArquivoUrl(caminhoUnico);
        guia.setDataInsercao(LocalDateTime.of(2026, 10, 1, 9, 0));
        guia.setUsuario(setor);
        guia.setDataValidadeAte(LocalDate.of(2099, 1, 1));
        guia = guiaRepository.save(guia);

        Assinatura assinatura = new Assinatura();
        assinatura.setUsuario(medico);
        String png = "assinaturas/medico-" + ARQUIVOS.incrementAndGet() + ".png";
        gravar(png, PdfsDeTeste.assinaturaPng());
        assinatura.setAssinaturaUrl(png);
        assinatura = assinaturaRepository.save(assinatura);

        salvarAssinaturaGuia(guia, AssinaturaGuiaPapel.MEDICO, assinatura);
        if (duasAssinaturas) {
            salvarAssinaturaGuia(guia, AssinaturaGuiaPapel.CHEFE, assinatura);
        }
        return guia;
    }

    private void salvarAssinaturaGuia(Guia guia, AssinaturaGuiaPapel papel, Assinatura assinatura) {
        AssinaturaGuia assinaturaGuia = new AssinaturaGuia();
        assinaturaGuia.setGuia(guia);
        assinaturaGuia.setPapel(papel);
        assinaturaGuia.setUsuario(medico);
        assinaturaGuia.setAssinatura(assinatura);
        assinaturaGuiaRepository.save(assinaturaGuia);
    }

    private static void gravar(String caminhoRelativo, byte[] conteudo) throws IOException {
        Path destino = RAIZ_ARQUIVOS.resolve(caminhoRelativo);
        Files.createDirectories(destino.getParent());
        Files.write(destino, conteudo);
    }

    private Usuario usuario(String login, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setSenhaHash("hash");
        u.setPerfil(perfil);
        u.setAtivo(true);
        return usuarioRepository.save(u);
    }

    private Beneficiario beneficiario(Usuario usuario, String precCp, String nome) {
        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome(nome);
        b.setPrecCp(precCp);
        b.setDataNascimento(LocalDate.of(1990, 1, 1));
        b.setTelefone("11999999991");
        return beneficiarioRepository.save(b);
    }
}
