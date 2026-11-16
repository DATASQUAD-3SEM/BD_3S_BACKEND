package fatec.fusex.nexus.guiaOcs;

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
import fatec.fusex.nexus.guia.SituacaoExecucao;
import fatec.fusex.nexus.guiaAssinatura.PdfsDeTeste;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimentoRepository;
import fatec.fusex.nexus.ocs.ContaOcs;
import fatec.fusex.nexus.ocs.ContaOcsRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.sessao.Sessao;
import fatec.fusex.nexus.sessao.SessaoRepository;
import fatec.fusex.nexus.sessao.SituacaoSessao;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de GET /ocs/guias/{id}/pdf (T19-BE4), com banco em memoria e arquivos no diretorio de testes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GuiaOcsPdfControllerTest {

    private static final LocalDate PASSADA = LocalDate.of(2020, 1, 1);
    private static final LocalDate FUTURA = LocalDate.of(2099, 1, 1);
    private static final String LOGIN_OCS = "ocs.lab";
    private static final String LOGIN_OUTRA_OCS = "ocs.outra";
    // Mesma raiz do nexus.storage.local-dir do perfil test (application-test.properties).
    private static final Path RAIZ_ARQUIVOS = Path.of("target", "uploads-test");
    private static final AtomicInteger ARQUIVOS = new AtomicInteger();

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final OcsRepository ocsRepository;
    private final ContaOcsRepository contaOcsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaProcedimentoRepository procedimentoRepository;
    private final SessaoRepository sessaoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AssinaturaGuiaRepository assinaturaGuiaRepository;

    private Ocs ocs;
    private Ocs outraOcs;
    private Usuario usuarioJoao;
    private Beneficiario joao;
    private Usuario setor;
    private Usuario medico;
    private int sequencia;

    GuiaOcsPdfControllerTest(MockMvc mockMvc,
                             UsuarioRepository usuarioRepository,
                             BeneficiarioRepository beneficiarioRepository,
                             OcsRepository ocsRepository,
                             ContaOcsRepository contaOcsRepository,
                             PreGuiaRepository preGuiaRepository,
                             GuiaRepository guiaRepository,
                             GuiaProcedimentoRepository procedimentoRepository,
                             SessaoRepository sessaoRepository,
                             AssinaturaRepository assinaturaRepository,
                             AssinaturaGuiaRepository assinaturaGuiaRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.ocsRepository = ocsRepository;
        this.contaOcsRepository = contaOcsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.sessaoRepository = sessaoRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
    }

    @BeforeEach
    void setUp() throws IOException {
        ocs = ocsRepository.save(novaOcs("Laboratorio Lab", "CTR-LAB"));
        outraOcs = ocsRepository.save(novaOcs("Clinica Outra", "CTR-OUT"));

        vincularConta(LOGIN_OCS, ocs);
        vincularConta(LOGIN_OUTRA_OCS, outraOcs);

        usuarioJoao = usuario("11111111111", PerfilUsuario.BENEFICIARIO);
        joao = beneficiario(usuarioJoao, "11111111111", "Joao Logado");
        setor = usuario("setor01", PerfilUsuario.SETOR_GUIAS);
        medico = usuario("medico01", PerfilUsuario.MEDICO);
    }

    @Test
    void devolvePdfSemCacheQuandoNaoHaAssinaturas() throws Exception {
        byte[] original = PdfsDeTeste.pdfComRotulos(true, true);
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA,
                SituacaoExecucao.ENVIADA, original);

        MvcResult resultado = mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("inline")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("guia-")))
                .andReturn();

        assertArrayEquals(original, resultado.getResponse().getContentAsByteArray());
    }

    @Test
    void arquivoGuardadoAusente_recebe404() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA,
                SituacaoExecucao.ENVIADA, PdfsDeTeste.pdfComRotulos(true, true));
        Files.delete(RAIZ_ARQUIVOS.resolve(guia.getArquivoUrl()));

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isNotFound());
    }

    @Test
    void comAssinaturaDoMedico_pdfVemCarimbado() throws Exception {
        byte[] original = PdfsDeTeste.pdfComRotulos(true, true);
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA,
                SituacaoExecucao.EM_EXECUCAO, original);
        registrarAssinaturaMedico(guia);

        MvcResult resultado = mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdf = resultado.getResponse().getContentAsByteArray();
        assertTrue(new String(pdf, 0, 4).startsWith("%PDF"));
        assertFalse(java.util.Arrays.equals(original, pdf), "a assinatura do medico deve alterar o PDF exibido");
        assertArrayEquals(original, Files.readAllBytes(RAIZ_ARQUIVOS.resolve(guia.getArquivoUrl())),
                "o arquivo guardado nao pode ser alterado");
    }

    @Test
    void guiaEncerrada_continuaDisponivelParaAOcs() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), PASSADA,
                SituacaoExecucao.EM_EXECUCAO, PdfsDeTeste.pdfComRotulos(true, true));
        Sessao assinada = criarSessao(criarProcedimento(guia, "41001109", 2), 1, SituacaoSessao.ASSINADA);
        assertTrue(assinada.getIdSessao() != null);

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    void guiaVencidaSemAssinatura_recebe404() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), PASSADA,
                SituacaoExecucao.ENVIADA, PdfsDeTeste.pdfComRotulos(true, true));

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isNotFound());
    }

    @Test
    void guiaDeOutraOcs_recebe404MesmoSabendoOId() throws Exception {
        Guia daOutra = criarGuia(outraOcs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA,
                SituacaoExecucao.ENVIADA, PdfsDeTeste.pdfComRotulos(true, true));

        mockMvc.perform(get(url(daOutra)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isNotFound());
    }

    @Test
    void guiaNaoEnviada_recebe404() throws Exception {
        Guia naoEnviada = criarGuiaNaoEnviada(ocs, PdfsDeTeste.pdfComRotulos(true, true));

        mockMvc.perform(get(url(naoEnviada)).header(SimulacaoLoginFilter.HEADER, LOGIN_OCS))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilDiferenteDeOcs_recebe403() throws Exception {
        Guia guia = criarGuia(ocs, LocalDateTime.of(2026, 10, 1, 10, 0), FUTURA,
                SituacaoExecucao.ENVIADA, PdfsDeTeste.pdfComRotulos(true, true));

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, "11111111111"))
                .andExpect(status().isForbidden());
    }

    // ---------- fixtures ----------

    private static String url(Guia guia) {
        return "/ocs/guias/" + guia.getIdGuia() + "/pdf";
    }

    private void registrarAssinaturaMedico(Guia guia) throws IOException {
        String caminhoPng = "assinaturas/medico-" + ARQUIVOS.incrementAndGet() + ".png";
        gravar(caminhoPng, PdfsDeTeste.assinaturaPng());

        Assinatura assinatura = new Assinatura();
        assinatura.setUsuario(medico);
        assinatura.setAssinaturaUrl(caminhoPng);
        assinatura = assinaturaRepository.save(assinatura);

        AssinaturaGuia assinaturaGuia = new AssinaturaGuia();
        assinaturaGuia.setGuia(guia);
        assinaturaGuia.setPapel(AssinaturaGuiaPapel.MEDICO);
        assinaturaGuia.setUsuario(medico);
        assinaturaGuia.setAssinatura(assinatura);
        assinaturaGuiaRepository.save(assinaturaGuia);
    }

    private Ocs novaOcs(String nome, String contrato) {
        Ocs o = new Ocs();
        o.setNome(nome);
        o.setContratoNum(contrato);
        o.setTipo(OcsTipo.OCS);
        return o;
    }

    private void vincularConta(String login, Ocs alvo) {
        Usuario conta = usuario(login, PerfilUsuario.OCS);
        ContaOcs contaOcs = new ContaOcs();
        contaOcs.setUsuario(conta);
        contaOcs.setOcs(alvo);
        contaOcsRepository.save(contaOcs);
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

    /** Cria guia de Joao enviada a OCS, gravando o PDF em disco no caminho guardado pela guia. */
    private Guia criarGuia(Ocs ocsDaGuia, LocalDateTime envio, LocalDate validade,
                           SituacaoExecucao situacao, byte[] pdf) throws IOException {
        Guia guia = novaGuia(ocsDaGuia, validade, situacao, pdf);
        guia.setDataEnvio(envio);
        guia.setEnviadaPor(usuarioJoao);
        return guiaRepository.save(guia);
    }

    private Guia criarGuiaNaoEnviada(Ocs ocsDaGuia, byte[] pdf) throws IOException {
        return guiaRepository.save(novaGuia(ocsDaGuia, FUTURA, SituacaoExecucao.NAO_ENVIADA, pdf));
    }

    private Guia novaGuia(Ocs ocsDaGuia, LocalDate validade, SituacaoExecucao situacao, byte[] pdf)
            throws IOException {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(PreGuiaStatus.CONCLUIDA);
        preGuia.setBeneficiario(joao);
        preGuia.setOcs(ocsDaGuia);
        preGuia.setEncaminhamentoUrl("encaminhamento.pdf");
        preGuia = preGuiaRepository.save(preGuia);

        String numero = String.format("%09d", ++sequencia);
        String caminho = "guias/guia-" + numero + "-" + ARQUIVOS.incrementAndGet() + ".pdf";
        gravar(caminho, pdf);

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(numero);
        guia.setArquivoUrl(caminho);
        guia.setDataInsercao(LocalDateTime.of(2026, 9, 1, 9, 0));
        guia.setUsuario(setor);
        guia.setDataValidadeAte(validade);
        guia.setSituacaoExecucao(situacao);
        return guia;
    }

    private static void gravar(String caminhoRelativo, byte[] conteudo) throws IOException {
        Path destino = RAIZ_ARQUIVOS.resolve(caminhoRelativo);
        Files.createDirectories(destino.getParent());
        Files.write(destino, conteudo);
    }

    private GuiaProcedimento criarProcedimento(Guia guia, String codDgp, int quantidade) {
        GuiaProcedimento p = new GuiaProcedimento();
        p.setGuia(guia);
        p.setCodDgp(codDgp);
        p.setDescricaoProcedimento("TC - Abdome superior");
        p.setQuantidadeAutorizada(quantidade);
        return procedimentoRepository.save(p);
    }

    private Sessao criarSessao(GuiaProcedimento procedimento, int numero, SituacaoSessao status) {
        Sessao s = new Sessao();
        s.setGuiaProcedimento(procedimento);
        s.setNumero(numero);
        s.setStatus(status);
        if (status != SituacaoSessao.DISPONIVEL) {
            s.setDataSolicitacao(LocalDateTime.of(2026, 10, 2, 9, 0));
            s.setSolicitadaPor(usuarioJoao);
        }
        if (status == SituacaoSessao.ASSINADA) {
            s.setDataAssinatura(LocalDateTime.of(2026, 10, 2, 10, 0));
            s.setAssinadoPor(usuarioJoao);
        }
        return sessaoRepository.save(s);
    }
}
