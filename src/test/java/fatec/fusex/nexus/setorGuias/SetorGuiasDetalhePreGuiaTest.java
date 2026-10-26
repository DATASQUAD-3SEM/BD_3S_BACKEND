package fatec.fusex.nexus.setorGuias;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.storage.ArmazenamentoArquivo;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guia.GuiaRepository;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunho;
import fatec.fusex.nexus.guiaRascunho.GuiaRascunhoRepository;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.ocs.Ocs;
import fatec.fusex.nexus.ocs.OcsRepository;
import fatec.fusex.nexus.ocs.OcsTipo;
import fatec.fusex.nexus.preGuia.PreGuia;
import fatec.fusex.nexus.preGuia.PreGuiaRepository;
import fatec.fusex.nexus.preGuia.PreGuiaStatus;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimento;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoId;
import fatec.fusex.nexus.preGuiaProcedimento.PreGuiaProcedimentoRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRepository;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameRolAns;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Testes de GET /setor-guias/pre-guias/{id} (T4-BE1): origem beneficiario, origem medico, rascunho e Guia concluida.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class SetorGuiasDetalhePreGuiaTest {

    private static final String HEADER = "X-Usuario-Login";
    private static final String SETOR = "setor.guias";
    private static final String BENEFICIARIO_LOGIN = "beneficiario.teste";

    private final MockMvc mockMvc;
    private final PreGuiaRepository preGuiaRepository;
    private final PreGuiaProcedimentoRepository preGuiaProcedimentoRepository;
    private final ProcedimentoExameRepository procedimentoExameRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaRascunhoRepository guiaRascunhoRepository;
    private final BeneficiarioRepository beneficiarioRepository;
    private final MedicoFusexRepository medicoFusexRepository;
    private final OcsRepository ocsRepository;
    private final UsuarioRepository usuarioRepository;
    private final ArmazenamentoArquivo armazenamentoArquivo;

    private Usuario setor;
    private Ocs ocs;
    private Beneficiario beneficiario;
    private MedicoFusex medico;

    SetorGuiasDetalhePreGuiaTest(
            MockMvc mockMvc,
            PreGuiaRepository preGuiaRepository,
            PreGuiaProcedimentoRepository preGuiaProcedimentoRepository,
            ProcedimentoExameRepository procedimentoExameRepository,
            GuiaRepository guiaRepository,
            GuiaRascunhoRepository guiaRascunhoRepository,
            BeneficiarioRepository beneficiarioRepository,
            MedicoFusexRepository medicoFusexRepository,
            OcsRepository ocsRepository,
            UsuarioRepository usuarioRepository,
            ArmazenamentoArquivo armazenamentoArquivo) {
        this.mockMvc = mockMvc;
        this.preGuiaRepository = preGuiaRepository;
        this.preGuiaProcedimentoRepository = preGuiaProcedimentoRepository;
        this.procedimentoExameRepository = procedimentoExameRepository;
        this.guiaRepository = guiaRepository;
        this.guiaRascunhoRepository = guiaRascunhoRepository;
        this.beneficiarioRepository = beneficiarioRepository;
        this.medicoFusexRepository = medicoFusexRepository;
        this.ocsRepository = ocsRepository;
        this.usuarioRepository = usuarioRepository;
        this.armazenamentoArquivo = armazenamentoArquivo;
    }

    @BeforeEach
    void setUp() {
        setor = novoUsuario(SETOR, PerfilUsuario.SETOR_GUIAS);
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
    @DisplayName("origem BENEFICIARIO: traz encaminhamento e nao traz medico criador")
    void origemBeneficiario() throws Exception {
        PreGuia pg = criarPreGuia(null, PreGuiaStatus.PENDENTE, salvarEncaminhamento("exame.pdf"));

        mockMvc.perform(get("/setor-guias/pre-guias/" + pg.getIdPreGuia()).header(HEADER, SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value(String.format("%04d", pg.getIdPreGuia())))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.origem").value("BENEFICIARIO"))
                .andExpect(jsonPath("$.beneficiario.nome").value("Marta Souza Lima"))
                .andExpect(jsonPath("$.beneficiario.precCp").value("45810020001"))
                .andExpect(jsonPath("$.ocsNome").value("Laboratorio Exemplo"))
                .andExpect(jsonPath("$.medicoCriador").value(nullValue()))
                .andExpect(jsonPath("$.encaminhamento.tipo").value("application/pdf"))
                .andExpect(jsonPath("$.encaminhamento.tamanhoBytes").value(14))
                .andExpect(jsonPath("$.guiaRascunho").value(nullValue()))
                .andExpect(jsonPath("$.guiaConcluida").value(false));
    }

    @Test
    @DisplayName("arquivo do encaminhamento ausente no armazenamento: o detalhe abre com tamanho 0")
    void encaminhamentoAusenteNoArmazenamento() throws Exception {
        PreGuia pg = criarPreGuia(null, PreGuiaStatus.PENDENTE, "encaminhamentos/nao-existe.jpg");

        mockMvc.perform(get("/setor-guias/pre-guias/" + pg.getIdPreGuia()).header(HEADER, SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encaminhamento.nome").value("nao-existe.jpg"))
                .andExpect(jsonPath("$.encaminhamento.tipo").value("image/jpeg"))
                .andExpect(jsonPath("$.encaminhamento.tamanhoBytes").value(0));
    }

    @Test
    @DisplayName("origem MEDICO: traz exames e medico criador, sem encaminhamento")
    void origemMedico() throws Exception {
        PreGuia pg = criarPreGuia(medico, PreGuiaStatus.EM_ANALISE, null);
        vincularExame(pg, "99999911", "Hemograma completo");
        vincularExame(pg, "99999912", "Glicose");

        mockMvc.perform(get("/setor-guias/pre-guias/" + pg.getIdPreGuia()).header(HEADER, SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origem").value("MEDICO"))
                .andExpect(jsonPath("$.medicoCriador.patente").value("Capitao"))
                .andExpect(jsonPath("$.medicoCriador.nome").value("Cap Med Ribeiro"))
                .andExpect(jsonPath("$.medicoCriador.crm").value("CRM-TESTE-1"))
                .andExpect(jsonPath("$.encaminhamento").value(nullValue()))
                .andExpect(jsonPath("$.procedimentos", hasSize(2)))
                .andExpect(jsonPath("$.procedimentos[0].codigoTuss").value("99999911"))
                .andExpect(jsonPath("$.procedimentos[0].nome").value("Hemograma completo"));
    }

    @Test
    @DisplayName("com rascunho de Guia: devolve numero e nome do arquivo")
    void comRascunho() throws Exception {
        PreGuia pg = criarPreGuia(null, PreGuiaStatus.EM_ANALISE, salvarEncaminhamento("exame.pdf"));

        GuiaRascunho rascunho = new GuiaRascunho();
        rascunho.setPreGuia(pg);
        rascunho.setNumero("202610701");
        rascunho.setArquivoUrl("guias-rascunho/teste.pdf");
        rascunho.setNomeArquivo("Guia_202610701.pdf");
        guiaRascunhoRepository.save(rascunho);

        mockMvc.perform(get("/setor-guias/pre-guias/" + pg.getIdPreGuia()).header(HEADER, SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guiaRascunho.numero").value("202610701"))
                .andExpect(jsonPath("$.guiaRascunho.nomeArquivo").value("Guia_202610701.pdf"))
                .andExpect(jsonPath("$.guiaConcluida").value(false));
    }

    @Test
    @DisplayName("com Guia criada: guiaConcluida = true")
    void guiaConcluida() throws Exception {
        PreGuia pg = criarPreGuia(null, PreGuiaStatus.EM_ANALISE, salvarEncaminhamento("exame.pdf"));

        Usuario setorEntidade = usuarioRepository.findByLogin(SETOR).orElseThrow();
        Guia guia = new Guia();
        guia.setPreGuia(pg);
        guia.setNumero("202610702");
        guia.setArquivoUrl("guias/teste.pdf");
        guia.setUsuario(setorEntidade);
        guiaRepository.save(guia);

        mockMvc.perform(get("/setor-guias/pre-guias/" + pg.getIdPreGuia()).header(HEADER, SETOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.guiaConcluida").value(true));
    }

    @Test
    @DisplayName("pre-guia inexistente: 404")
    void naoExiste() throws Exception {
        mockMvc.perform(get("/setor-guias/pre-guias/999999").header(HEADER, SETOR))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("perfil diferente de SETOR_GUIAS: 403")
    void perfilErrado() throws Exception {
        PreGuia pg = criarPreGuia(null, PreGuiaStatus.PENDENTE, salvarEncaminhamento("exame.pdf"));

        mockMvc.perform(get("/setor-guias/pre-guias/" + pg.getIdPreGuia()).header(HEADER, BENEFICIARIO_LOGIN))
                .andExpect(status().isForbidden());
    }

    // ---------- auxiliares ----------

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

    private String salvarEncaminhamento(String nome) {
        // 14 bytes: o teste confere tamanhoBytes = 14
        return armazenamentoArquivo.salvar(
                new MockMultipartFile("arquivo", nome, "application/pdf", "conteudo-teste".getBytes()),
                "encaminhamentos");
    }

    private void vincularExame(PreGuia pg, String codigo, String nome) {
        ProcedimentoExame p = new ProcedimentoExame();
        p.setCodigoTuss(codigo);
        p.setTerminologiaProcedimento(nome);
        p.setRolAns(ProcedimentoExameRolAns.NAO);
        p = procedimentoExameRepository.save(p);

        preGuiaProcedimentoRepository.save(new PreGuiaProcedimento(
                new PreGuiaProcedimentoId(pg.getIdPreGuia(), p.getIdProcedimentoExame()),
                pg,
                p));
    }
}