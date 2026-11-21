package fatec.fusex.nexus.guiaAssinatura;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.assinatura.Assinatura;
import fatec.fusex.nexus.assinatura.AssinaturaRepository;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuia;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaPapel;
import fatec.fusex.nexus.assinaturaGuia.AssinaturaGuiaRepository;
import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
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

/**
 * T30-BE3: o PDF da Guia (GET /guias/{id}/pdf e GET /beneficiarios/me/pre-guias/{id}/guia/pdf) e a correcao da
 * pre-guia seguem a regra unica de quem age no paciente: titular (proprio ou menor sob a guarda dele), responsavel
 * legal (menor sob responsavel externo) e o proprio dependente maior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class GuiaPdfPacienteDependenteTest {

    private static final Path RAIZ_ARQUIVOS = Path.of("target", "uploads-test");
    private static final AtomicInteger ARQUIVOS = new AtomicInteger();
    private static final String LOGIN_TITULAR = "52998224760";
    private static final String LOGIN_RESPONSAVEL = "41282945061";
    private static final String LOGIN_MAIOR = "98765432162";

    private final MockMvc mockMvc;
    private final UsuarioRepository usuarioRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final DependenteRepository dependenteRepository;
    private final OcsRepository ocsRepository;
    private final PreGuiaRepository preGuiaRepository;
    private final GuiaRepository guiaRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final AssinaturaGuiaRepository assinaturaGuiaRepository;

    private Beneficiario titular;
    private Usuario responsavel;
    private Usuario contaMaior;
    private Dependente miguel;
    private Dependente sofia;
    private Dependente carla;
    private Usuario setor;
    private Usuario medico;
    private Ocs ocs;
    private int sequencia;

    GuiaPdfPacienteDependenteTest(MockMvc mockMvc,
                                  UsuarioRepository usuarioRepository,
                                  BeneficiarioRepository beneficiarioRepository,
                                  DependenteRepository dependenteRepository,
                                  OcsRepository ocsRepository,
                                  PreGuiaRepository preGuiaRepository,
                                  GuiaRepository guiaRepository,
                                  AssinaturaRepository assinaturaRepository,
                                  AssinaturaGuiaRepository assinaturaGuiaRepository) {
        this.mockMvc = mockMvc;
        this.usuarioRepository = usuarioRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.dependenteRepository = dependenteRepository;
        this.ocsRepository = ocsRepository;
        this.preGuiaRepository = preGuiaRepository;
        this.guiaRepository = guiaRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.assinaturaGuiaRepository = assinaturaGuiaRepository;
    }

    @BeforeEach
    void setUp() {
        ocs = new Ocs();
        ocs.setNome("Laboratorio Paciente");
        ocs.setContratoNum("CTR-PACIENTE-PDF");
        ocs.setTipo(OcsTipo.OCS);
        ocs = ocsRepository.save(ocs);

        Usuario contaTitular = usuario(LOGIN_TITULAR, PerfilUsuario.BENEFICIARIO);
        titular = new Beneficiario();
        titular.setUsuario(contaTitular);
        titular.setNome("Antonio Paciente");
        titular.setPrecCp("45872213360");
        titular.setDataNascimento(LocalDate.of(1968, 3, 15));
        titular.setTelefone("12987654321");
        titular = beneficiarioRepository.save(titular);

        responsavel = usuario(LOGIN_RESPONSAVEL, PerfilUsuario.RESPONSAVEL_EXTERNO);
        contaMaior = usuario(LOGIN_MAIOR, PerfilUsuario.BENEFICIARIO);
        setor = usuario("setor.paciente.pdf", PerfilUsuario.SETOR_GUIAS);
        medico = usuario("medico.paciente.pdf", PerfilUsuario.MEDICO);

        miguel = dependente("Miguel Paciente", "39053344761", "45872213361", TipoDependente.MENOR_TITULAR,
                null, null, LocalDate.of(2048, 3, 20));
        sofia = dependente("Sofia Paciente", "39053344762", "45872213362", TipoDependente.MENOR_RESPONSAVEL,
                null, responsavel, LocalDate.of(2039, 5, 14));
        carla = dependente("Carla Paciente", "98765432162", "45872213363", TipoDependente.MAIOR,
                contaMaior, null, null);
    }

    @Test
    void titularAbreOPdfDaPropriaGuiaEDaGuiaDoMenorSobAGuardaDele() throws Exception {
        mockMvc.perform(get(url(guia(null, PreGuiaStatus.CONCLUIDA))).header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isOk());
        mockMvc.perform(get(url(guia(miguel, PreGuiaStatus.CONCLUIDA))).header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isOk());
    }

    @Test
    void titularRecebe403NoPdfDoMenorSobResponsavelEDoDependenteMaior() throws Exception {
        mockMvc.perform(get(url(guia(sofia, PreGuiaStatus.CONCLUIDA))).header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(url(guia(carla, PreGuiaStatus.CONCLUIDA))).header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isForbidden());
    }

    @Test
    void responsavelAbreOPdfDoMenorQueAcompanhaPelasDuasRotas() throws Exception {
        Guia guia = guia(sofia, PreGuiaStatus.CONCLUIDA);

        mockMvc.perform(get(url(guia)).header(SimulacaoLoginFilter.HEADER, LOGIN_RESPONSAVEL))
                .andExpect(status().isOk());
        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/guia/pdf", guia.getPreGuia().getIdPreGuia())
                        .param("modo", "visualizar")
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RESPONSAVEL))
                .andExpect(status().isOk());
    }

    @Test
    void responsavelRecebe403NoPdfDeOutroPaciente() throws Exception {
        mockMvc.perform(get(url(guia(miguel, PreGuiaStatus.CONCLUIDA))).header(SimulacaoLoginFilter.HEADER, LOGIN_RESPONSAVEL))
                .andExpect(status().isForbidden());
    }

    @Test
    void dependenteMaiorAbreOPdfDaPropriaGuia() throws Exception {
        mockMvc.perform(get(url(guia(carla, PreGuiaStatus.CONCLUIDA))).header(SimulacaoLoginFilter.HEADER, LOGIN_MAIOR))
                .andExpect(status().isOk());
    }

    @Test
    void correcaoDaPreGuiaDoMenorSobResponsavel_soParaOResponsavel() throws Exception {
        Guia guia = guia(sofia, PreGuiaStatus.ATENCAO);
        Long idPreGuia = guia.getPreGuia().getIdPreGuia();

        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/correcao", idPreGuia)
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_RESPONSAVEL))
                .andExpect(status().isOk());
        mockMvc.perform(get("/beneficiarios/me/pre-guias/{id}/correcao", idPreGuia)
                        .header(SimulacaoLoginFilter.HEADER, LOGIN_TITULAR))
                .andExpect(status().isNotFound());
    }

    // ---------- fixtures ----------

    private static String url(Guia guia) {
        return "/guias/" + guia.getIdGuia() + "/pdf";
    }

    /** Guia com as duas assinaturas (medico e chefe), da pre-guia do titular ou do dependente informado. */
    private Guia guia(Dependente dependente, PreGuiaStatus status) throws IOException {
        PreGuia preGuia = new PreGuia();
        preGuia.setStatus(status);
        preGuia.setBeneficiario(titular);
        preGuia.setDependente(dependente);
        preGuia.setOcs(ocs);
        preGuia.setEncaminhamentoUrl("encaminhamento.pdf");
        preGuia.setDataCriacao(LocalDateTime.of(2026, 10, 1, 9, 0));
        preGuia = preGuiaRepository.save(preGuia);

        String caminho = "guias/paciente-" + ARQUIVOS.incrementAndGet() + ".pdf";
        gravar(caminho, PdfsDeTeste.pdfComRotulos(true, true));

        Guia guia = new Guia();
        guia.setPreGuia(preGuia);
        guia.setNumero(String.format("%09d", 700000 + ++sequencia));
        guia.setArquivoUrl(caminho);
        guia.setDataInsercao(LocalDateTime.of(2026, 10, 1, 9, 0));
        guia.setUsuario(setor);
        guia.setDataValidadeAte(LocalDate.of(2099, 1, 1));
        guia = guiaRepository.save(guia);

        Assinatura assinatura = new Assinatura();
        assinatura.setUsuario(medico);
        String png = "assinaturas/paciente-" + ARQUIVOS.incrementAndGet() + ".png";
        gravar(png, PdfsDeTeste.assinaturaPng());
        assinatura.setAssinaturaUrl(png);
        assinatura = assinaturaRepository.save(assinatura);

        salvarAssinaturaGuia(guia, AssinaturaGuiaPapel.MEDICO, assinatura);
        salvarAssinaturaGuia(guia, AssinaturaGuiaPapel.CHEFE, assinatura);
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

    private Dependente dependente(String nome, String cpf, String precCp, TipoDependente tipo, Usuario conta,
                                  Usuario responsavelLegal, LocalDate vigenciaFim) {
        Dependente d = new Dependente();
        d.setTitular(titular);
        d.setUsuario(conta);
        d.setResponsavel(responsavelLegal);
        d.setNome(nome);
        d.setDataNascimento(tipo == TipoDependente.MAIOR ? LocalDate.of(1985, 2, 8) : LocalDate.of(2017, 5, 15));
        d.setCpf(cpf);
        d.setPrecCp(precCp);
        d.setTipo(tipo);
        d.setVigenciaFim(vigenciaFim);
        return dependenteRepository.save(d);
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
}
